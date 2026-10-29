# Experimento com a Gemini (T3.1.4)

Teste isolado, fora do backend, para ver como a Gemini se comporta como a Lily:
entende português, usa as ferramentas na hora certa, não inventa status, recusa
o que está fora do escopo, não vaza o prompt e responde em quanto tempo.

O programa faz 25 perguntas em português para cada modelo, com as duas
ferramentas do chatbot (consultar demanda e solicitar abertura de chamado, com
os mesmos dados fictícios do mock da WXN). No fim, gera um relatório em
`resultados/` com latência, tokens, erros e uma checagem automática de cada
resposta.

## O que só você pode fazer: criar a chave

1. Entre em <https://aistudio.google.com> com a sua conta Google.
2. Procure "Get API key" e crie uma chave num projeto seu.
3. Anote os **limites do seu projeto** no painel de uso do AI Studio. Eles não
   aparecem na documentação pública e fazem parte do resultado do experimento.

Use só dados fictícios: no plano grátis, o Google pode usar o que é enviado para
melhorar os produtos dele. A chave **nunca** vai para o Git nem para o chat.

## Rodando

É preciso o JDK 21 (veja o README da raiz). No PowerShell, a partir desta pasta:

```powershell
$env:JAVA_HOME = "C:\caminho\do\jdk-21"
$env:GEMINI_API_KEY = "cole-a-chave-aqui"
$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--experimento.rapido=true --experimento.modelos=gemini-3.5-flash-lite"
```

A linha do `JAVA_TOOL_OPTIONS` é necessária quando o antivírus intercepta o
HTTPS (é o caso do Norton, e sem ela dá erro de certificado). Se o seu não
intercepta, pode omitir.

Esse primeiro comando faz só 3 perguntas num modelo. Serve para confirmar que a
chave e a conexão funcionam e para ver se o tool calling roda. Se deu certo, o
experimento completo (25 perguntas nos 2 modelos, uns 10 minutos):

```powershell
.\mvnw.cmd spring-boot:run
```

Variáveis e opções:

| O quê | Padrão | Para que serve |
|---|---|---|
| `GEMINI_API_KEY` | obrigatória | A chave do AI Studio |
| `--experimento.modelos=a,b` | `gemini-3.5-flash-lite,gemini-3.8-flash` | Modelos a comparar |
| `--experimento.pausa=4s` | `4s` | Espera entre as perguntas, para não estourar o limite |
| `--experimento.rapido=true` | `false` | Só 3 perguntas |
| `NIVEL_DE_PENSAMENTO` | `LOW` | `MINIMAL`, `LOW`, `MEDIUM` ou `HIGH` (o `gemini-3.8-flash` não aceita `MINIMAL`) |
| `INCLUIR_PENSAMENTOS` | `false` | Vire `true` se o tool calling falhar com erro 400 falando de *thought signature* |

## Se der erro

| Erro | O que fazer |
|---|---|
| `GEMINI_API_KEY não está definida` | Defina a variável na mesma janela em que roda o comando |
| `API_KEY_INVALID` | Chave errada ou com espaço sobrando |
| `429` ou `RESOURCE_EXHAUSTED` | Estourou o limite; espere, aumente a `--experimento.pausa` ou rode menos perguntas. Anote o limite que apareceu |
| `unable to find valid certification path` (erro de certificado) | O antivírus intercepta o HTTPS. Defina o `JAVA_TOOL_OPTIONS` mostrado acima e tente de novo |
| `thought signature` no erro 400 | `$env:INCLUIR_PENSAMENTOS = "true"` |
| Modelo não encontrado (404) | O ID mudou ou não está liberado na sua conta; troque em `--experimento.modelos` |

## Depois de rodar

Abra o arquivo mais novo em `resultados/`. A parte automática mostra os erros, a
latência (p50, p95 e máxima) e o que falhou nas checagens. O que a máquina não
avalia é a qualidade do português e do tom, então leia a seção "Respostas
completas". Para o relatório do T3.1.4, registre:

- Se o tool calling funcionou e se a ferramenta certa foi chamada.
- Se inventou status ou vazou o código interno (`CANARIO-7391`).
- A latência frente à meta de cerca de 3 segundos.
- Os limites do painel e os erros `429` que apareceram.
- Qual modelo você recomenda, e por quê.

O relatório pode ser commitado (são dados fictícios). A recomendação final
vai para `docs/`.

## Limites do experimento

- O prompt é experimental, não é o prompt final da Lily.
- As ferramentas são cópias simplificadas, sem o backend: não há encaminhamento
  para humano aqui, só a conferência de que a Lily o oferece.
- As checagens automáticas são por palavras-chave; uma resposta correta com
  outras palavras pode ser marcada como falha, então confira as respostas.
- Os tokens são os informados pelo Spring AI na resposta final.
