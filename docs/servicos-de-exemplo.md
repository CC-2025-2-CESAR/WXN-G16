# Serviços de exemplo na base da Lily

Para a demonstração a Lily precisa saber responder "quais serviços vocês oferecem?". A base de conhecimento
(`backend/src/main/resources/conhecimento/base-wxn.md`) não trazia nenhum serviço da WXN, então a Lily só dizia que a WXN
"é uma software house que integra sistemas a ERPs" e oferecia um atendente.

A seção **"Serviços da WXN (dados de exemplo para a demonstração)"** da base foi escrita para isso. **É dado de exemplo**: a
lista foi montada a partir do que empresas parecidas costumam anunciar e dos dados de demonstração do projeto (notas fiscais,
módulo financeiro, planilhas de demanda), e **não vem da WXN**. Antes de usar com um cliente de verdade, troque pelo catálogo
real.

## Onde editar

Só o arquivo `backend/src/main/resources/conhecimento/base-wxn.md` (a seção de serviços, logo depois de "Quem é a WXN"). Nada
mais precisa mudar: o backend lê o arquivo ao subir. Dá para apontar para outro arquivo com `chatbot.base-de-conhecimento`.

Regras que valem ao editar:

- A Lily só diz o que está escrito na base. O que não estiver ali, ela responde que não sabe e oferece um atendente.
- Não coloque preços, prazos, números de clientes, certificações nem contatos que não sejam verdadeiros: ela repete o que
  estiver escrito.
- Mantenha a seção curta (a base inteira entra no prompt de toda dúvida, e a cota da Gemini é pequena). Hoje são uns 1.400
  caracteres.
- A última linha da seção ("Para escopo, valores e prazos... oferece um atendente") mantém a Lily coerente com a regra de que
  ela não informa preço nem prazo.

## De onde vieram as ideias

Buscas na web (22/11/2026), usando só o **tipo de serviço** que cada empresa anuncia, com palavras nossas e **sem copiar
texto**. Foram lidos os resultados da busca, não as páginas inteiras. Preços, prazos, números de clientes e certificações dessas
empresas foram ignorados.

- Fornecedores de API fiscal e integração de notas para software houses (emissão e consulta de NF-e e NFS-e): nfe.io
  (<https://nfe.io/segmentos/software-house-erps/>), Tecnospeed (<https://blog.tecnospeed.com.br/plugnotas/>), Qive
  (<https://qive.com.br/blog/integracao-sistemas-gestao-fiscal>), Focus NFe (<https://focusnfe.com.br/casos-de-sucesso/kadan/>).
  Deram o serviço "Integração de notas fiscais" e os modelos de integração (API, arquivos, banco).
- Integradoras de ERP no Brasil: BOA e EZ4 (consultoria, suporte, implantação, customização e integração de Protheus e RM,
  <https://itforum.com.br/?p=173370> e <https://portalerp.com/br/empresa/prime-consultoria>) e a documentação de integração
  do Protheus (<https://tdn.totvs.com/x/w2qlGw>). Deram "Desenvolvimento sob medida", "Suporte e sustentação" e a ideia de
  integração entre módulos do ERP.
- Integração de ERP e migração de dados: OpenText (<https://www.opentext.com/br/solutions/erp-integration-and-transformation>)
  e Innowise (<https://innowise.com/pt/servicos/erp/integracao>). Deram "Migração e saneamento de dados".
- Casos de BI integrado a ERP (Totvs Fast Analytics, <https://fronteiradagestao.substack.com/p/case-de-implantacao-de-erp>).
  Deu "BI e painéis".
- "Automação de processos" e "Módulo financeiro" vêm dos dados de demonstração do próprio projeto (planilhas de demanda,
  "módulo financeiro bloqueado depois da troca de senha").

## Como testar

Com a Lily ligada (Gemini; no modo simulado ela não usa a base), pergunte:

1. "Quais serviços vocês oferecem?" — esperado: lista os serviços da seção.
2. "Quanto custa a integração de notas fiscais?" — esperado: não informa valor e oferece um atendente.
3. "Vocês fazem consultoria de marketing?" — esperado: diz que não tem essa informação (não está na lista) e oferece um atendente.
