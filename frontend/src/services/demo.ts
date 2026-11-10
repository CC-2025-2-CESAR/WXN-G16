import type { Atendente, ConversaNaFila, DetalheConversaFila, MensagemResumo, TipoRemetente } from '../types/fila';

type Fala = [TipoRemetente, string];
type Cenario = {
  id: string;
  nome: string;
  telefone: string;
  minutosAtras: number;
  motivo: string;
  falas: Fala[];
};

// Mesmos sete casos fictícios cadastrados pelo perfil demo do backend.
const cenarios: Cenario[] = [
  {
    id: 'demo-0101', nome: 'Marcos Albuquerque', telefone: '81 99999-0101', minutosAtras: 118,
    motivo: 'Relatório de vendas com valores divergentes, urgente para a reunião de hoje',
    falas: [
      ['CLIENTE', 'Boa tarde. O relatório de vendas do ERP está com valores diferentes do fechamento do mês.'],
      ['CHATBOT', 'Boa tarde! Sinto muito pelo transtorno. Você já tem algum protocolo aberto para isso?'],
      ['CLIENTE', 'Não tenho. Preciso do relatório correto para a reunião de hoje às 16h.'],
      ['CHATBOT', 'Entendi que é urgente. Vou chamar um atendente para te ajudar, tudo bem?'],
      ['CLIENTE', 'Sim, por favor.'],
    ],
  },
  {
    id: 'demo-0102', nome: 'Juliana Teixeira', telefone: '81 99999-0102', minutosAtras: 86,
    motivo: 'Quer saber o andamento da integração de notas fiscais e não tem o protocolo',
    falas: [
      ['CLIENTE', 'Oi, queria saber como está a integração de emissão de notas fiscais.'],
      ['CHATBOT', 'Claro! Qual é o protocolo da demanda?'],
      ['CLIENTE', 'Não lembro. Pedi isso por telefone na semana passada.'],
      ['CHATBOT', 'Sem o protocolo eu não consigo localizar a demanda com segurança. Posso chamar um atendente que busque pelo seu cadastro?'],
      ['CLIENTE', 'Pode sim.'],
    ],
  },
  {
    id: 'demo-0103', nome: 'Roberto Carvalho', telefone: '81 99999-0103', minutosAtras: 64,
    motivo: 'Emissão de nota fiscal trava na etapa de assinatura desde ontem',
    falas: [
      ['CLIENTE', 'A emissão de nota fiscal trava na assinatura desde ontem.'],
      ['CHATBOT', 'Sinto muito por isso. Aparece alguma mensagem de erro na tela?'],
      ['CLIENTE', 'Aparece "tempo esgotado" e a nota fica parada. Já tentei umas dez vezes.'],
      ['CHATBOT', 'Obrigada pelos detalhes. Como isso está parando a emissão das suas notas, vou chamar um atendente agora.'],
    ],
  },
  {
    id: 'demo-0104', nome: 'Fernanda Lima', telefone: '81 99999-0104', minutosAtras: 41,
    motivo: 'Quer tratar da renovação do contrato com uma pessoa',
    falas: [
      ['CLIENTE', 'Quero falar sobre a renovação do meu contrato.'],
      ['CHATBOT', 'Eu consigo ajudar com o status de demandas e a abertura de chamados. Para renovação de contrato, o melhor é falar com um atendente. Posso encaminhar?'],
      ['CLIENTE', 'Prefiro falar com uma pessoa mesmo.'],
    ],
  },
  {
    id: 'demo-0105', nome: 'Diego Barros', telefone: '81 99999-0105', minutosAtras: 27,
    motivo: 'Módulo financeiro bloqueado depois da troca de senha',
    falas: [
      ['CLIENTE', 'Troquei a senha hoje e agora o módulo financeiro não abre.'],
      ['CHATBOT', 'Entendi. Os outros módulos abrem normalmente para você?'],
      ['CLIENTE', 'Abrem, só o financeiro que não.'],
      ['CHATBOT', 'Parece um problema de permissão de acesso, e isso eu não consigo resolver por aqui. Vou chamar um atendente.'],
    ],
  },
  {
    id: 'demo-0106', nome: 'Patrícia Santana', telefone: '81 99999-0106', minutosAtras: 13,
    motivo: 'Diz já ter enviado a planilha da demanda WXN-2026-0003, que consta como aguardando retorno',
    falas: [
      ['CLIENTE', 'Qual é o status da demanda WXN-2026-0003?'],
      ['CHATBOT', 'A demanda WXN-2026-0003, de cadastro de novos centros de custo, está aguardando retorno: falta o envio da planilha de centros de custo.'],
      ['CLIENTE', 'Mas eu já mandei a planilha por e-mail na segunda-feira!'],
      ['CHATBOT', 'Entendo a confusão. Não consigo confirmar o recebimento do e-mail por aqui, então vou chamar um atendente para verificar com você.'],
    ],
  },
  {
    id: 'demo-0107', nome: 'Luís Henrique Duarte', telefone: '81 99999-0107', minutosAtras: 4,
    motivo: 'Quer cancelar um chamado aberto por engano',
    falas: [
      ['CLIENTE', 'Abri um chamado por engano agora há pouco e preciso cancelar.'],
      ['CHATBOT', 'Eu não consigo cancelar chamados por aqui. Posso encaminhar você para um atendente?'],
      ['CLIENTE', 'Pode, é rápido.'],
    ],
  },
];

const atendentes: Atendente[] = [
  { id: 'demo-ana', nome: 'Ana Martins', email: 'ana.martins@demo.wxn.example', disponivel: true },
  { id: 'demo-paulo', nome: 'Paulo Mendes', email: 'paulo.mendes@demo.wxn.example', disponivel: true },
  { id: 'demo-carla', nome: 'Carla Nogueira', email: 'carla.nogueira@demo.wxn.example', disponivel: false },
];
const criadoEm = Date.now();
const aguardando = new Set(cenarios.map((cenario) => cenario.id));
const assumidas = new Set<string>();
const abertoEm = (cenario: Cenario) => new Date(criadoEm - cenario.minutosAtras * 60_000).toISOString();

export function listarDemoFila(): ConversaNaFila[] {
  return cenarios.filter((cenario) => aguardando.has(cenario.id)).map((cenario, index) => ({
    posicao: index + 1,
    conversaId: cenario.id,
    clienteNome: cenario.nome,
    clienteTelefone: cenario.telefone,
    motivo: cenario.motivo,
    abertoEm: abertoEm(cenario),
  }));
}

export function abrirDemoConversa(id: string): DetalheConversaFila | null {
  const cenario = cenarios.find((item) => item.id === id && aguardando.has(item.id));
  if (!cenario) return null;
  const horario = new Date(abertoEm(cenario)).getTime();
  const falas: Fala[] = [
    ['CHATBOT', 'Oi, eu sou a Lily, assistente virtual da WXN. Como posso ajudar?'],
    ...cenario.falas,
  ];
  const mensagens: MensagemResumo[] = falas.map(([tipoRemetente, conteudo], index) => ({
    tipoRemetente,
    conteudo,
    enviadaEm: new Date(horario - (falas.length - index) * 40_000).toISOString(),
  }));
  return {
    conversaId: cenario.id,
    clienteNome: cenario.nome,
    clienteTelefone: cenario.telefone,
    motivo: cenario.motivo,
    abertoEm: abertoEm(cenario),
    mensagens,
  };
}

export function listarDemoAtendentes(): Atendente[] {
  return atendentes.map((atendente) => ({ ...atendente }));
}

export function assumirDemoConversa(conversaId: string, atendenteId: string): 'ok' | 'conversa-ausente' | 'atendente-ausente' | 'atendente-indisponivel' {
  if (!aguardando.has(conversaId)) return 'conversa-ausente';
  const atendente = atendentes.find((item) => item.id === atendenteId);
  if (!atendente) return 'atendente-ausente';
  if (!atendente.disponivel) return 'atendente-indisponivel';
  aguardando.delete(conversaId);
  assumidas.add(conversaId);
  return 'ok';
}

export function encerrarDemoConversa(conversaId: string): boolean {
  if (!assumidas.has(conversaId)) return false;
  assumidas.delete(conversaId);
  return true;
}
