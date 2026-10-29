import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Seed de DESENVOLVIMENTO: documentos da matrícula (RN12) para os alunos do seed.sql.
 * Envia um PDF fictício por documento ao bucket privado "documentos-alunos" e grava documento_aluno.
 *
 * Distribuição (próxima do que acontece numa escola pequena):
 *  - veteranos (primeira matrícula antes de 2026): quase todos completos; ~8% faltando 1 documento;
 *  - novos de 2026: ~65% completos; o resto faltando 1 a 3;
 *  - novos de 2027 (aprovados na pré-matrícula): entregaram só 1 ou 2 documentos;
 *  - declaração de adimplência (não obrigatória): ~20% dos alunos novos (vieram de escola particular).
 * Alunos que já têm algum documento são ignorados (pode rodar de novo sem duplicar).
 *
 * Uso (na raiz do projeto, com o driver do PostgreSQL do repositório Maven local):
 *   java -cp ~/.m2/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar supabase/scripts/SeedDocumentos.java .env
 */
public class SeedDocumentos {

    private static final String BUCKET = "documentos-alunos";
    private static final List<String> OBRIGATORIOS = List.of("IDENTIDADE_RESPONSAVEIS", "CERTIDAO_NASCIMENTO",
            "CARTEIRA_VACINACAO", "COMPROVANTE_RESIDENCIA", "FOTOS_3X4", "AUTORIZADOS_BUSCAR");
    private static final String ADIMPLENCIA = "DECLARACAO_ADIMPLENCIA";

    record Aluno(UUID id, long numero, int primeiroAno) {
    }

    record Documento(UUID alunoId, String tipo, String caminho, OffsetDateTime enviadoEm) {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> env = lerEnv(Path.of(args.length > 0 ? args[0] : ".env"));
        String supabase = env.get("SUPABASE_URL");
        String chave = env.get("SUPABASE_SECRET_KEY");
        byte[] pdf = pdfFicticio();

        try (Connection c = DriverManager.getConnection(env.get("SUPABASE_DB_URL"), env.get("SUPABASE_DB_USER"),
                env.get("SUPABASE_DB_PASSWORD"))) {
            List<Aluno> alunos = new ArrayList<>();
            try (PreparedStatement st = c.prepareStatement("""
                    SELECT a.id, a.numero_cadastro, min(al.ano) AS primeiro
                      FROM aluno a
                      JOIN matricula m ON m.aluno_id = a.id
                      JOIN ano_letivo al ON al.id = m.ano_letivo_id
                     WHERE NOT EXISTS (SELECT 1 FROM documento_aluno d WHERE d.aluno_id = a.id)
                     GROUP BY a.id, a.numero_cadastro
                     ORDER BY a.numero_cadastro""");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    alunos.add(new Aluno(rs.getObject(1, UUID.class), rs.getLong(2), rs.getInt(3)));
                }
            }
            List<Documento> documentos = new ArrayList<>();
            for (Aluno a : alunos) {
                documentos.addAll(planejar(a));
            }
            System.out.printf("Alunos sem documentos: %d. Documentos a enviar: %d%n", alunos.size(), documentos.size());

            HttpClient http = HttpClient.newHttpClient();
            ExecutorService pool = Executors.newFixedThreadPool(8);
            List<Future<Boolean>> envios = new ArrayList<>();
            for (Documento d : documentos) {
                envios.add(pool.submit(() -> enviar(http, supabase, chave, d.caminho(), pdf)));
            }
            List<Documento> enviados = new ArrayList<>();
            for (int i = 0; i < documentos.size(); i++) {
                if (envios.get(i).get()) {
                    enviados.add(documentos.get(i));
                }
            }
            pool.shutdown();

            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO documento_aluno (aluno_id, tipo, arquivo_url, enviado_em) VALUES (?, ?, ?, ?)")) {
                for (Documento d : enviados) {
                    ins.setObject(1, d.alunoId());
                    ins.setString(2, d.tipo());
                    ins.setString(3, d.caminho());
                    ins.setObject(4, d.enviadoEm());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            c.commit();
            System.out.printf("Gravados: %d (falhas no envio: %d)%n", enviados.size(), documentos.size() - enviados.size());
        }
    }

    /** Decide quais documentos o aluno entregou, de forma determinística (mesmo aluno, mesmo resultado). */
    static List<Documento> planejar(Aluno a) {
        Random r = new Random(a.numero() * 7919L);
        List<String> tipos = new ArrayList<>(OBRIGATORIOS);
        Collections.shuffle(tipos, r);
        int faltando;
        LocalDate base;
        if (a.primeiroAno() >= 2027) {
            faltando = OBRIGATORIOS.size() - 1 - r.nextInt(2);           // entregou 1 ou 2
            base = LocalDate.of(2026, 10, 8);
        } else if (a.primeiroAno() == 2026) {
            faltando = r.nextInt(100) < 65 ? 0 : 1 + r.nextInt(3);
            base = LocalDate.of(2025, 11, 3);
        } else {
            faltando = r.nextInt(100) < 8 ? 1 : 0;
            base = LocalDate.of(a.primeiroAno() - 1, 11, 3);
        }
        List<String> entregues = new ArrayList<>(tipos.subList(0, tipos.size() - faltando));
        if (a.primeiroAno() >= 2026 && r.nextInt(100) < 20) {
            entregues.add(ADIMPLENCIA);
        }
        int janela = a.primeiroAno() >= 2027 ? 18 : 75;
        List<Documento> lista = new ArrayList<>();
        for (String tipo : entregues) {
            LocalDate dia = base.plusDays(r.nextInt(janela));
            OffsetDateTime quando = dia.atTime(8 + r.nextInt(9), r.nextInt(60)).atOffset(ZoneOffset.ofHours(-3));
            String caminho = a.id() + "/" + tipo.toLowerCase(Locale.ROOT).replace('_', '-') + "-" + UUID.randomUUID() + ".pdf";
            lista.add(new Documento(a.id(), tipo, caminho, quando));
        }
        return lista;
    }

    static boolean enviar(HttpClient http, String supabase, String chave, String caminho, byte[] pdf) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(supabase + "/storage/v1/object/" + BUCKET + "/" + caminho))
                    .header("apikey", chave)
                    .header("Content-Type", "application/pdf")
                    .header("x-upsert", "false")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(pdf))
                    .build();
            int status = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status / 100 != 2) {
                System.err.println("Falha no envio (HTTP " + status + ")");
            }
            return status / 100 == 2;
        } catch (Exception e) {
            System.err.println("Falha no envio: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /** PDF de uma página com o aviso de documento fictício (xref com deslocamentos corretos). */
    static byte[] pdfFicticio() {
        String texto = "BT /F1 18 Tf 72 720 Td (Sistema Semear - documento ficticio) Tj 0 -28 Td /F1 12 Tf "
                + "(Arquivo gerado para testes. Nao contem dados reais.) Tj ET";
        String[] objetos = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length " + texto.length() + " >>\nstream\n" + texto + "\nendstream"
        };
        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.length; i++) {
            offsets.add(sb.length());
            sb.append(i + 1).append(" 0 obj\n").append(objetos[i]).append("\nendobj\n");
        }
        int xref = sb.length();
        sb.append("xref\n0 ").append(objetos.length + 1).append("\n0000000000 65535 f \n");
        for (int o : offsets) {
            sb.append(String.format("%010d 00000 n \n", o));
        }
        sb.append("trailer\n<< /Size ").append(objetos.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    static Map<String, String> lerEnv(Path arquivo) throws Exception {
        Map<String, String> env = new HashMap<>();
        for (String linha : Files.readAllLines(arquivo)) {
            int i = linha.indexOf('=');
            if (i > 0 && !linha.startsWith("#")) {
                env.put(linha.substring(0, i).trim(), linha.substring(i + 1).trim());
            }
        }
        return env;
    }
}
