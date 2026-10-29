package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.TipoDocumentoAluno;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.ArquivoEnviado;

class DocumentosTest {

    private static final byte[] PDF = "%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private final UUID aluno = UUID.randomUUID();

    @Test
    void reconheceOFormatoPeloConteudo() {
        assertThat(new ArquivoEnviado(PDF).getFormato()).isEqualTo(ArquivoEnviado.Formato.PDF);
        assertThat(new ArquivoEnviado(PNG).getFormato().getContentType()).isEqualTo("image/png");
        assertThat(new ArquivoEnviado(JPEG).getFormato().getExtensao()).isEqualTo("jpg");
    }

    @Test
    void recusaArquivoDisfarcadoVazioOuGrande() {
        assertThatThrownBy(() -> new ArquivoEnviado("MZ executavel".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(DadoInvalidoException.class).hasMessageContaining("PDF");
        assertThatThrownBy(() -> new ArquivoEnviado(new byte[0])).isInstanceOf(DadoInvalidoException.class);
        byte[] grande = Arrays.copyOf(PDF, ArquivoEnviado.TAMANHO_MAXIMO + 1);
        assertThatThrownBy(() -> new ArquivoEnviado(grande)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void caminhoNoBucketNaoUsaNomeOriginal() {
        DocumentoAluno doc = DocumentoAluno.novo(aluno, TipoDocumentoAluno.CARTEIRA_VACINACAO, new ArquivoEnviado(PDF));
        assertThat(doc.getCaminho()).startsWith(aluno + "/carteira-vacinacao-").endsWith(".pdf");
        assertThat(doc.getExtensao()).isEqualTo("pdf");
    }

    private DocumentoAluno enviado(TipoDocumentoAluno tipo) {
        return new DocumentoAluno(UUID.randomUUID(), aluno, tipo, aluno + "/x.pdf", OffsetDateTime.now());
    }

    @Test
    void rn12_semDocumentosFaltamOsSeisObrigatorios() {
        ChecklistDocumentos checklist = new ChecklistDocumentos(List.of());
        assertThat(checklist.getItens()).hasSize(7);
        assertThat(checklist.pendentes()).hasSize(6).doesNotContain(TipoDocumentoAluno.DECLARACAO_ADIMPLENCIA);
        assertThat(checklist.estaCompleto()).isFalse();
    }

    @Test
    void rn12_completoComTodosOsObrigatorios() {
        List<DocumentoAluno> todos = Arrays.stream(TipoDocumentoAluno.values())
                .filter(TipoDocumentoAluno::isObrigatorio)
                .map(this::enviado)
                .toList();
        assertThat(new ChecklistDocumentos(todos).estaCompleto()).isTrue();
    }

    @Test
    void rn12_variosArquivosParaOMesmoItem() {
        ChecklistDocumentos checklist = new ChecklistDocumentos(List.of(
                enviado(TipoDocumentoAluno.IDENTIDADE_RESPONSAVEIS), enviado(TipoDocumentoAluno.IDENTIDADE_RESPONSAVEIS)));
        assertThat(checklist.getItens().get(0).documentos()).hasSize(2);
        assertThat(checklist.pendentes()).hasSize(5);
    }
}
