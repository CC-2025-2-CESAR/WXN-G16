import type { Atendente, ConversaNaFila, DetalheConversaFila, EmAtendimento, MensagemResumo, NivelUrgencia, TipoRemetente } from '../types/fila';
import type { HistoricoDetalhe, HistoricoItem } from '../types/historico';
import type { RespostaChat, StatusConversa } from '../simulador/tipos';

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
type VinculoDemo = { atendenteId: string; assumidaEm: number };
type ConversaDemoSimulada = {
  id: string;
  nome: string;
  telefone: string;
  motivo: string;
  abertoEm: string;
  urgencia: NivelUrgencia;
  status: StatusConversa;
  mensagens: MensagemResumo[];
};
type EstadoDemo = {
  criadoEm: number;
  assumidas: Record<string, VinculoDemo>;
  respostas: Record<string, MensagemResumo[]>;
  simuladas?: Record<string, ConversaDemoSimulada>;
  encerradas?: Record<string, HistoricoDetalhe>;
};

const mensagemGenerica = (texto: string) =>
  /^(oi|ol[aá]|bom dia|boa tarde|boa noite)([,!.?\s]+(tudo bem|como vai)[!?.,\s]*)?[!?.,\s]*$/i.test(texto) ||
  /^(quero|gostaria de|preciso)\s+(falar com|de)\s+(um[a]?\s+)?(atendente|humano|humana|pessoa)[!?.,\s]*$/i.test(texto);
const CHAVE_DEMO = 'wxn-demo-conversas';
function lerEstado(): EstadoDemo | null {
  try {
    const bruto = sessionStorage.getItem(CHAVE_DEMO);
    return bruto ? JSON.parse(bruto) as EstadoDemo : null;
  } catch {
    return null;
  }
}
const estado = lerEstado();
const criadoEm = typeof estado?.criadoEm === 'number' ? estado.criadoEm : Date.now();
const exemplosEncerrados: Array<{
  id: string;
  nome: string;
  telefone: string;
  motivo: string;
  horasAtras: number;
  duracaoMinutos: number;
  atendenteId: string;
  resumo: string;
  falas: Fala[];
}> = [
  {
    id: 'demo-hist-01', nome: 'Beatriz Melo', telefone: '81 99999-0201',
    motivo: 'Acesso ao portal bloqueado após atualização de senha', horasAtras: 1, duracaoMinutos: 34,
    atendenteId: 'demo-ana', resumo: 'Acesso restabelecido após atualização das permissões do perfil.',
    falas: [
      ['CLIENTE', 'Troquei minha senha e agora não consigo entrar no portal.'],
      ['CHATBOT', 'Entendi. Vou chamar um atendente para verificar seu acesso.'],
      ['ATENDENTE', 'Olá, Beatriz! Vou conferir as permissões do seu perfil.'],
      ['ATENDENTE', 'Ajustei as permissões. Pode tentar acessar novamente?'],
      ['CLIENTE', 'Funcionou, obrigada!'],
    ],
  },
  {
    id: 'demo-hist-02', nome: 'Carlos Nunes', telefone: '81 99999-0202',
    motivo: 'Dúvida sobre o prazo do chamado WXN-2026-0024', horasAtras: 28, duracaoMinutos: 42,
    atendenteId: 'demo-paulo', resumo: 'Prazo confirmado e atualização enviada ao cliente.',
    falas: [
      ['CLIENTE', 'Queria saber quando o chamado WXN-2026-0024 será concluído.'],
      ['CHATBOT', 'Vou encaminhar sua consulta para um atendente.'],
      ['ATENDENTE', 'Olá, Carlos. Vou verificar o andamento com a equipe.'],
      ['ATENDENTE', 'A previsão é concluir até sexta-feira. Registrei essa atualização no chamado.'],
      ['CLIENTE', 'Certo, obrigado pelo retorno.'],
    ],
  },
  {
    id: 'demo-hist-03', nome: 'Renata Lima', telefone: '81 99999-0203',
    motivo: 'Erro ao exportar relatório financeiro em PDF', horasAtras: 76, duracaoMinutos: 51,
    atendenteId: 'demo-ana', resumo: 'Exportação normalizada após ajuste no modelo do relatório.',
    falas: [
      ['CLIENTE', 'O relatório financeiro não está exportando para PDF.'],
      ['CHATBOT', 'Obrigada por avisar. Vou encaminhar o problema para a equipe.'],
      ['ATENDENTE', 'Oi, Renata. Consegue me informar o período usado no relatório?'],
      ['CLIENTE', 'Estou tentando exportar o mês passado.'],
      ['ATENDENTE', 'Ajustamos o modelo. Fiz o teste com o mesmo período e a exportação foi concluída.'],
    ],
  },
  {
    id: 'demo-hist-04', nome: 'João Santos', telefone: '81 99999-0204',
    motivo: 'Atualização dos dados de contato da empresa', horasAtras: 112, duracaoMinutos: 26,
    atendenteId: 'demo-paulo', resumo: 'Dados de contato atualizados e confirmados com o cliente.',
    falas: [
      ['CLIENTE', 'Preciso atualizar o telefone da nossa empresa no cadastro.'],
      ['CHATBOT', 'Vou chamar um atendente para fazer essa atualização com você.'],
      ['ATENDENTE', 'Olá, João. Pode confirmar o novo telefone de contato?'],
      ['CLIENTE', 'Sim, é o número que enviei no cadastro.'],
      ['ATENDENTE', 'Atualização realizada. Obrigado pela confirmação!'],
    ],
  },
];

function montarExemploEncerrado(exemplo: (typeof exemplosEncerrados)[number]): HistoricoDetalhe {
  const encerrada = criadoEm - exemplo.horasAtras * 3_600_000;
  const iniciada = encerrada - exemplo.duracaoMinutos * 60_000;
  const atendente = atendentes.find((item) => item.id === exemplo.atendenteId)!;
  const mensagens = exemplo.falas.map(([tipoRemetente, conteudo], indice) => ({
    tipoRemetente,
    conteudo,
    enviadaEm: new Date(iniciada + (indice + 1) * (encerrada - iniciada) / (exemplo.falas.length + 1)).toISOString(),
  }));
  return {
    conversaId: exemplo.id,
    clienteNome: exemplo.nome,
    clienteTelefone: exemplo.telefone,
    motivo: exemplo.motivo,
    origem: 'SIMULADOR',
    urgencia: 'BAIXA',
    atendenteId: atendente.id,
    atendenteNome: atendente.nome,
    iniciadaEm: new Date(iniciada).toISOString(),
    assumidaEm: new Date(iniciada + 7 * 60_000).toISOString(),
    encerradaEm: new Date(encerrada).toISOString(),
    resumoEncerramento: exemplo.resumo,
    totalMensagens: mensagens.length,
    mensagens,
  };
}

const encerradas = new Map<string, HistoricoDetalhe>(
  Object.entries(estado?.encerradas ?? {}).filter(([id, conversa]) =>
    id === conversa?.conversaId &&
    typeof conversa.clienteNome === 'string' &&
    typeof conversa.encerradaEm === 'string' &&
    Array.isArray(conversa.mensagens),
  ),
);
for (const [id, conversa] of encerradas) {
  if (conversa.origem === 'DEMONSTRACAO') encerradas.set(id, { ...conversa, origem: 'SIMULADOR' });
}
for (const exemplo of exemplosEncerrados) {
  if (!encerradas.has(exemplo.id)) encerradas.set(exemplo.id, montarExemploEncerrado(exemplo));
}
const simuladas = new Map<string, ConversaDemoSimulada>(
  Object.entries(estado?.simuladas ?? {}).filter(([id, conversa]) =>
    id.startsWith('demo-sim-') &&
    conversa?.id === id &&
    typeof conversa.telefone === 'string' &&
    typeof conversa.nome === 'string' &&
    Array.isArray(conversa.mensagens) &&
    ['COM_CHATBOT', 'AGUARDANDO_ATENDENTE', 'COM_ATENDENTE'].includes(conversa.status),
  ),
);
const assumidas = new Map<string, VinculoDemo>(
  Object.entries(estado?.assumidas ?? {}).filter(([id, vinculo]) =>
    (cenarios.some((c) => c.id === id) || simuladas.has(id)) &&
    !encerradas.has(id) &&
    atendentes.some((a) => a.id === vinculo?.atendenteId) &&
    typeof vinculo?.assumidaEm === 'number',
  ),
);
const aguardando = new Set([
  ...cenarios.map((cenario) => cenario.id).filter((id) => !assumidas.has(id) && !encerradas.has(id)),
  ...[...simuladas.values()].filter((conversa) => conversa.status === 'AGUARDANDO_ATENDENTE' && !assumidas.has(conversa.id)).map((conversa) => conversa.id),
]);
const respostas = new Map<string, MensagemResumo[]>(
  Object.entries(estado?.respostas ?? {}).filter(([id, mensagens]) => assumidas.has(id) && Array.isArray(mensagens)),
);
function salvarEstado() {
  try {
    sessionStorage.setItem(CHAVE_DEMO, JSON.stringify({
      criadoEm,
      assumidas: Object.fromEntries(assumidas),
      respostas: Object.fromEntries(respostas),
      simuladas: Object.fromEntries(simuladas),
      encerradas: Object.fromEntries(encerradas),
    } satisfies EstadoDemo));
  } catch {
    // A demonstração continua nesta página se o armazenamento estiver indisponível.
  }
}
function avisarMudanca() {
  window.dispatchEvent(new Event('wxn:demo-conversations-changed'));
}
const abertoEm = (cenario: Cenario) => new Date(criadoEm - cenario.minutosAtras * 60_000).toISOString();

const ORDEM_DA_URGENCIA: Record<NivelUrgencia, number> = { ALTA: 0, MEDIA: 1, BAIXA: 2 };

// Só para a demonstração: o backend calcula a urgência de verdade ao abrir o ticket.
const urgenciaDoCenario = (cenario: Cenario): NivelUrgencia =>
  /trava|bloquead|parou|urgente/i.test(cenario.motivo)
    ? 'ALTA'
    : /cancelar|andamento|planilha/i.test(cenario.motivo)
      ? 'BAIXA'
      : 'MEDIA';

const paraFila = (conversa: ConversaDemoSimulada): ConversaNaFila => ({
  posicao: 0,
  conversaId: conversa.id,
  clienteNome: conversa.nome,
  clienteTelefone: conversa.telefone,
  motivo: conversa.motivo,
  abertoEm: conversa.abertoEm,
  origem: 'SIMULADOR',
  urgencia: conversa.urgencia,
});

function detalheSimulada(conversa: ConversaDemoSimulada): DetalheConversaFila {
  return {
    ...paraFila(conversa),
    mensagens: [...conversa.mensagens, ...(respostas.get(conversa.id) ?? [])].sort(
      (a, b) => Date.parse(a.enviadaEm) - Date.parse(b.enviadaEm),
    ),
  };
}

/** Permite ao celular da prévia retomar as respostas escritas pelo atendente. */
export function consultarDemoConversa(telefone: string) {
  const conversa = [...simuladas.values()].find((item) => item.telefone === telefone);
  if (!conversa) return null;
  return {
    conversaId: conversa.id,
    status: conversa.status,
    respostas: (respostas.get(conversa.id) ?? []).map((mensagem) => ({ ...mensagem })),
  };
}

export function eConversaSimuladaDemo(conversaId: string) {
  return simuladas.has(conversaId);
}

/** Prévia local: respostas de exemplo, sem chamada à API ou consulta a dados da WXN. */
export function enviarDemoParaLily(entrada: { telefone: string; nome: string; texto: string }): RespostaChat {
  const telefone = entrada.telefone.replace(/\D/g, '');
  const texto = entrada.texto.trim();
  const agora = new Date().toISOString();
  let conversa = [...simuladas.values()].find((item) => item.telefone === telefone);
  if (!conversa) {
    conversa = {
      id: `demo-sim-${crypto.randomUUID()}`,
      nome: entrada.nome.trim() || `Cliente ${telefone.slice(-4)}`,
      telefone,
      motivo: '',
      abertoEm: agora,
      urgencia: 'BAIXA',
      status: 'COM_CHATBOT',
      mensagens: [],
    };
    simuladas.set(conversa.id, conversa);
  }

  conversa.mensagens.push({ tipoRemetente: 'CLIENTE', conteudo: texto, enviadaEm: agora });
  let resposta: string | null = null;
  if (conversa.status === 'COM_CHATBOT') {
    const pedidoHumano = /\b(atendente|humano|humana|pessoa|suporte)\b|falar\s+com\s+(algu[eé]m|uma pessoa)|abrir\s+(um\s+)?chamado/i.test(texto);
    const urgente = /urgente|parou|caiu|fora do ar|bloquead|travou|n[aã]o funciona|empresa est[aá] parada/i.test(texto);
    if (pedidoHumano || urgente) {
      conversa.motivo = [...conversa.mensagens].reverse().find((mensagem) =>
        mensagem.tipoRemetente === 'CLIENTE' && !mensagemGenerica(mensagem.conteudo),
      )?.conteudo ?? 'Solicitou atendimento humano';
      conversa.urgencia = urgente ? 'ALTA' : 'MEDIA';
      conversa.status = 'AGUARDANDO_ATENDENTE';
      aguardando.add(conversa.id);
      resposta = '[Simulação local] Entendi. Coloquei sua conversa na fila para um atendente continuar com você.';
    } else if (/\b(status|protocolo|demanda)\b/i.test(texto)) {
      resposta = '[Simulação local] Não consultei dados reais. Para verificar um protocolo, informe mais detalhes ou peça um atendente.';
    } else {
      resposta = '[Simulação local] Recebi sua mensagem. Posso ajudar com uma dúvida ou chamar um atendente.';
    }
    conversa.mensagens.push({ tipoRemetente: 'CHATBOT', conteudo: resposta, enviadaEm: new Date().toISOString() });
  }
  salvarEstado();
  avisarMudanca();
  return { conversaId: conversa.id, resposta, status: conversa.status };
}

/** Como o backend: mais urgentes primeiro e, entre iguais, quem pediu antes. */
export function listarDemoFila(): ConversaNaFila[] {
  const fixas = cenarios
    .filter((cenario) => aguardando.has(cenario.id))
    .map((cenario) => ({
      posicao: 0,
      conversaId: cenario.id,
      clienteNome: cenario.nome,
      clienteTelefone: cenario.telefone,
      motivo: cenario.motivo,
      abertoEm: abertoEm(cenario),
      origem: 'DEMONSTRACAO' as const,
      urgencia: urgenciaDoCenario(cenario),
    }));
  const novas = [...simuladas.values()].filter((conversa) => aguardando.has(conversa.id)).map(paraFila);
  return [...fixas, ...novas]
    .sort((a, b) => ORDEM_DA_URGENCIA[a.urgencia] - ORDEM_DA_URGENCIA[b.urgencia] || Date.parse(a.abertoEm) - Date.parse(b.abertoEm))
    .map((conversa, index) => ({ ...conversa, posicao: index + 1 }));
}

function mensagensDoCenario(cenario: Cenario): MensagemResumo[] {
  const horario = new Date(abertoEm(cenario)).getTime();
  const falas: Fala[] = [
    ['CHATBOT', 'Oi, eu sou a Lily, assistente virtual da WXN. Como posso ajudar?'],
    ...cenario.falas,
  ];
  return falas.map(([tipoRemetente, conteudo], index) => ({
    tipoRemetente,
    conteudo,
    enviadaEm: new Date(horario - (falas.length - index) * 40_000).toISOString(),
  }));
}

function detalheDoCenario(cenario: Cenario): DetalheConversaFila {
  const mensagens = mensagensDoCenario(cenario);
  return {
    conversaId: cenario.id,
    clienteNome: cenario.nome,
    clienteTelefone: cenario.telefone,
    motivo: cenario.motivo,
    abertoEm: abertoEm(cenario),
    origem: 'DEMONSTRACAO',
    mensagens,
  };
}

export function abrirDemoConversa(id: string): DetalheConversaFila | null {
  const simulada = simuladas.get(id);
  if (simulada && aguardando.has(id)) return detalheSimulada(simulada);
  const cenario = cenarios.find((item) => item.id === id && aguardando.has(item.id));
  return cenario ? detalheDoCenario(cenario) : null;
}

export function listarDemoAtendentes(): Atendente[] {
  return atendentes.map((atendente) => ({ ...atendente }));
}

export function listarDemoAtendimentosAtivos(atendenteId: string): EmAtendimento[] {
  const atendente = atendentes.find((item) => item.id === atendenteId);
  if (!atendente) return [];
  const fixos = cenarios.flatMap((cenario) => {
    const vinculo = assumidas.get(cenario.id);
    if (!vinculo || vinculo.atendenteId !== atendenteId) return [];
    const detalhe = detalheDoCenario(cenario);
    detalhe.mensagens.push(...(respostas.get(cenario.id) ?? []));
    return [{ detalhe, atendente: { ...atendente }, assumidaEm: vinculo.assumidaEm }];
  });
  const novas = [...simuladas.values()].flatMap((conversa) => {
    const vinculo = assumidas.get(conversa.id);
    if (!vinculo || vinculo.atendenteId !== atendenteId) return [];
    return [{ detalhe: detalheSimulada(conversa), atendente: { ...atendente }, assumidaEm: vinculo.assumidaEm }];
  });
  return [...fixos, ...novas].sort((a, b) => (b.assumidaEm ?? 0) - (a.assumidaEm ?? 0));
}

export function assumirDemoConversa(conversaId: string, atendenteId: string): 'ok' | 'conversa-ausente' | 'atendente-ausente' | 'atendente-indisponivel' {
  if (!aguardando.has(conversaId)) return 'conversa-ausente';
  const atendente = atendentes.find((item) => item.id === atendenteId);
  if (!atendente) return 'atendente-ausente';
  if (!atendente.disponivel) return 'atendente-indisponivel';
  aguardando.delete(conversaId);
  assumidas.set(conversaId, { atendenteId, assumidaEm: Date.now() });
  const simulada = simuladas.get(conversaId);
  if (simulada) simulada.status = 'COM_ATENDENTE';
  salvarEstado();
  avisarMudanca();
  return 'ok';
}

export function encerrarDemoConversa(conversaId: string, resumoEncerramento?: string): boolean {
  const vinculo = assumidas.get(conversaId);
  if (!vinculo) return false;
  const atendente = atendentes.find((item) => item.id === vinculo.atendenteId);
  const simulada = simuladas.get(conversaId);
  const cenario = cenarios.find((item) => item.id === conversaId);
  if (!atendente || (!simulada && !cenario)) return false;
  const detalhe = simulada ? detalheSimulada(simulada) : detalheDoCenario(cenario!);
  if (cenario) detalhe.mensagens.push(...(respostas.get(conversaId) ?? []));
  detalhe.mensagens.sort((a, b) => Date.parse(a.enviadaEm) - Date.parse(b.enviadaEm));
  encerradas.set(conversaId, {
    conversaId,
    clienteNome: detalhe.clienteNome,
    clienteTelefone: detalhe.clienteTelefone,
    motivo: detalhe.motivo,
    origem: detalhe.origem === 'DEMONSTRACAO' ? 'SIMULADOR' : detalhe.origem,
    urgencia: simulada ? simulada.urgencia : urgenciaDoCenario(cenario!),
    atendenteId: atendente.id,
    atendenteNome: atendente.nome,
    iniciadaEm: detalhe.abertoEm,
    assumidaEm: new Date(vinculo.assumidaEm).toISOString(),
    encerradaEm: new Date().toISOString(),
    resumoEncerramento: resumoEncerramento?.trim() || null,
    totalMensagens: detalhe.mensagens.length,
    mensagens: detalhe.mensagens.map((mensagem) => ({ ...mensagem })),
  });
  assumidas.delete(conversaId);
  respostas.delete(conversaId);
  simuladas.delete(conversaId);
  salvarEstado();
  avisarMudanca();
  return true;
}

/** Histórico local da prévia, da conversa encerrada mais recente para a mais antiga. */
export function listarDemoHistorico(): HistoricoItem[] {
  return [...encerradas.values()]
    .sort((a, b) => Date.parse(b.encerradaEm) - Date.parse(a.encerradaEm))
    .map(({ mensagens: _mensagens, ...conversa }) => ({ ...conversa }));
}

export function detalharDemoHistorico(conversaId: string): HistoricoDetalhe | null {
  const conversa = encerradas.get(conversaId);
  return conversa ? { ...conversa, mensagens: conversa.mensagens.map((mensagem) => ({ ...mensagem })) } : null;
}

/** Histórico completo de uma conversa em atendimento: falas do cenário mais as respostas enviadas. */
export function listarDemoMensagensDoAtendimento(conversaId: string): MensagemResumo[] | null {
  const simulada = simuladas.get(conversaId);
  if (simulada && assumidas.has(conversaId)) return detalheSimulada(simulada).mensagens.map((mensagem) => ({ ...mensagem }));
  const cenario = cenarios.find((item) => item.id === conversaId);
  if (!cenario || !assumidas.has(conversaId)) return null;
  return [...mensagensDoCenario(cenario), ...(respostas.get(conversaId) ?? [])].map((mensagem) => ({ ...mensagem }));
}

export function enviarDemoMensagem(conversaId: string, texto: string): MensagemResumo | 'conversa-ausente' | 'texto-invalido' {
  if (!assumidas.has(conversaId)) return 'conversa-ausente';
  const conteudo = texto.trim();
  if (!conteudo || conteudo.length > 4000) return 'texto-invalido';
  const mensagem: MensagemResumo = { tipoRemetente: 'ATENDENTE', conteudo, enviadaEm: new Date().toISOString() };
  respostas.set(conversaId, [...(respostas.get(conversaId) ?? []), mensagem]);
  salvarEstado();
  avisarMudanca();
  return { ...mensagem };
}
