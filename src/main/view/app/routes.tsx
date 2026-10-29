import { Navigate, createBrowserRouter } from 'react-router'
import { ProtectedRoute, RedirecionarPorPerfil } from '@/auth/ProtectedRoute'
import { EscolaLayout } from '@/layouts/EscolaLayout'
import { PortalLayout } from '@/layouts/PortalLayout'
import { PublicLayout } from '@/layouts/PublicLayout'
import { TurmaDetalhePage } from '@/pages/escola/academico/TurmaDetalhePage'
import { TurmasPage } from '@/pages/escola/academico/TurmasPage'
import { MateriaisPage } from '@/pages/escola/config/MateriaisPage'
import { PerfisAcessoPage } from '@/pages/escola/config/PerfisAcessoPage'
import { ObrigacoesPage } from '@/pages/escola/institucional/ObrigacoesPage'
import { CalendarioPage } from '@/pages/escola/pedagogico/CalendarioPage'
import { TurmaPedagogicaPage } from '@/pages/escola/pedagogico/TurmaPedagogicaPage'
import { TurmasPedagogicasPage } from '@/pages/escola/pedagogico/TurmasPedagogicasPage'
import { ContratoPage } from '@/pages/escola/matricula/ContratoPage'
import { ContratoPortalPage } from '@/pages/portal/ContratoPortalPage'
import { FilhoPortalPage } from '@/pages/portal/FilhoPortalPage'
import { BolsasPage } from '@/pages/escola/financeiro/BolsasPage'
import { CobrancasPage } from '@/pages/escola/financeiro/CobrancasPage'
import { InadimplenciaPage } from '@/pages/escola/financeiro/InadimplenciaPage'
import { ResumoFinanceiroPage } from '@/pages/escola/financeiro/ResumoFinanceiroPage'
import { CobrancaPortalPage } from '@/pages/portal/CobrancaPortalPage'
import { FinanceiroPortalPage } from '@/pages/portal/FinanceiroPortalPage'
import { InicioEscolaPage } from '@/pages/escola/InicioEscolaPage'
import { MatriculaDetalhePage } from '@/pages/escola/matricula/MatriculaDetalhePage'
import { MatriculasPage } from '@/pages/escola/matricula/MatriculasPage'
import { PreMatriculaDetalhePage } from '@/pages/escola/matricula/PreMatriculaDetalhePage'
import { PreMatriculasPage } from '@/pages/escola/matricula/PreMatriculasPage'
import { DocumentosFilhoPage } from '@/pages/portal/DocumentosFilhoPage'
import { NovaPreMatriculaPage } from '@/pages/portal/NovaPreMatriculaPage'
import { RematriculaPage } from '@/pages/portal/RematriculaPage'
import { AlunoDetalhePage } from '@/pages/escola/pessoa/AlunoDetalhePage'
import { AlunosPage } from '@/pages/escola/pessoa/AlunosPage'
import { PendenciasPage } from '@/pages/escola/pessoa/PendenciasPage'
import { ResponsaveisPage } from '@/pages/escola/pessoa/ResponsaveisPage'
import { ResponsavelDetalhePage } from '@/pages/escola/pessoa/ResponsavelDetalhePage'
import { FuncionariosPage } from '@/pages/escola/rh/FuncionariosPage'
import { NaoEncontradaPage } from '@/pages/NaoEncontradaPage'
import { InicioPortalPage } from '@/pages/portal/InicioPortalPage'
import { CadastroPage } from '@/pages/publico/CadastroPage'
import { LoginPage } from '@/pages/publico/LoginPage'
import { RecuperarSenhaPage } from '@/pages/publico/RecuperarSenhaPage'
import { RedefinirSenhaPage } from '@/pages/publico/RedefinirSenhaPage'

// Rotas da View (docs/05-view.md §4). Novas telas entram como filhas de /escola ou /portal.
export const router = createBrowserRouter([
  { path: '/', element: <RedirecionarPorPerfil /> },
  {
    element: <PublicLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/cadastro', element: <CadastroPage /> },
      { path: '/recuperar-senha', element: <RecuperarSenhaPage /> },
      { path: '/redefinir-senha', element: <RedefinirSenhaPage /> },
    ],
  },
  {
    path: '/escola',
    element: (
      <ProtectedRoute area="escola">
        <EscolaLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <InicioEscolaPage /> },
      { path: 'alunos', element: <AlunosPage /> },
      { path: 'alunos/:id', element: <AlunoDetalhePage /> },
      { path: 'responsaveis', element: <ResponsaveisPage /> },
      { path: 'responsaveis/:id', element: <ResponsavelDetalhePage /> },
      { path: 'turmas', element: <TurmasPage /> },
      { path: 'turmas/:id', element: <TurmaDetalhePage /> },
      { path: 'rh/funcionarios', element: <FuncionariosPage /> },
      { path: 'pre-matriculas', element: <PreMatriculasPage /> },
      { path: 'pre-matriculas/:id', element: <PreMatriculaDetalhePage /> },
      { path: 'matriculas', element: <MatriculasPage /> },
      { path: 'matriculas/:id', element: <MatriculaDetalhePage /> },
      { path: 'pendencias', element: <PendenciasPage /> },
      { path: 'financeiro', element: <Navigate to="/escola/financeiro/cobrancas" replace /> },
      { path: 'financeiro/cobrancas', element: <CobrancasPage /> },
      { path: 'financeiro/inadimplencia', element: <InadimplenciaPage /> },
      { path: 'financeiro/resumo', element: <ResumoFinanceiroPage /> },
      { path: 'financeiro/bolsas', element: <BolsasPage /> },
      { path: 'config/perfis', element: <PerfisAcessoPage /> },
      { path: 'config/materiais', element: <MateriaisPage /> },
      { path: 'obrigacoes', element: <ObrigacoesPage /> },
      { path: 'pedagogico', element: <TurmasPedagogicasPage /> },
      { path: 'pedagogico/turmas/:id', element: <TurmaPedagogicaPage /> },
      { path: 'pedagogico/calendario', element: <CalendarioPage /> },
      { path: 'contratos/:id', element: <ContratoPage /> },
      { path: '*', element: <NaoEncontradaPage /> },
    ],
  },
  {
    path: '/portal',
    element: (
      <ProtectedRoute area="portal">
        <PortalLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <InicioPortalPage /> },
      { path: 'pre-matricula/nova', element: <NovaPreMatriculaPage /> },
      { path: 'rematricula', element: <RematriculaPage /> },
      { path: 'alunos/:id', element: <FilhoPortalPage /> },
      { path: 'alunos/:id/documentos', element: <DocumentosFilhoPage /> },
      { path: 'contratos/:id', element: <ContratoPortalPage /> },
      { path: 'financeiro', element: <FinanceiroPortalPage /> },
      { path: 'cobrancas/:id', element: <CobrancaPortalPage /> },
      { path: '*', element: <NaoEncontradaPage /> },
    ],
  },
  { path: '*', element: <NaoEncontradaPage /> },
])
