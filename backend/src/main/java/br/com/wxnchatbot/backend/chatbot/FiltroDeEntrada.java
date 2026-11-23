package br.com.wxnchatbot.backend.chatbot;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class FiltroDeEntrada {

    /** Abaixo do que a API de chat aceita (4000) e do que o Telegram entrega (4096), mas acima de uma mensagem normal. */
    static final int TAMANHO_MAXIMO = 2000;

    public static final String RESPOSTA_MUITO_LONGA = "Sua mensagem ficou muito longa. Pode resumir em poucas linhas o que você precisa?";
    public static final String RESPOSTA_MANIPULACAO = "Não consigo mudar a forma como eu atendo. Posso ajudar com o status de demandas, a abertura de chamados ou chamar um atendente.";

    // Só barra pedido CLARO de mudar ou revelar as instruções da Lily ("suas instruções", "as regras anteriores",
    // "prompt de sistema"). Palavras soltas como "instruções", "regras" ou "prompt" aparecem em perguntas normais de
    // suporte ("me mostre as instruções de configuração", "esqueça as regras antigas, quero abrir um chamado").
    private static final String VERBOS = "(ignore|ignora|ignorar|esqueca|esquece|esquecer|desconsidere|desconsidera"
            + "|revele|revela|mostre|mostra|repita|repete|diga|me passe)";
    private static final String ALVO = "(instrucoes|instrucao|regras|ordens|prompt)";
    private static final String DO_ASSISTENTE = "(seu|sua|suas|seus|teu|tua|teus|tuas)";
    private static final String ANTERIORES = "(anteriores|acima|previas|iniciais|originais|que (te|lhe) (deram|passaram)|que voce recebeu)";

    private static final Pattern MANIPULACAO = Pattern.compile(
            "\\b" + VERBOS + "\\b.{0,30}\\b" + DO_ASSISTENTE + " " + ALVO + "\\b"
                    + "|\\b" + VERBOS + "\\b.{0,30}\\b" + ALVO + " (todas )?" + ANTERIORES + "\\b"
                    + "|\\bprompt (de|do) sistema\\b|\\bsystem prompt\\b"
                    + "|\\b(a partir de agora|de agora em diante) voce (e|sera|vai ser)\\b"
                    + "|\\bignore (all |the )?(previous|prior|above) instructions\\b");

    private FiltroDeEntrada() {
    }

    public static Optional<String> barrar(String texto) {
        if (texto.length() > TAMANHO_MAXIMO) {
            return Optional.of(RESPOSTA_MUITO_LONGA);
        }
        if (MANIPULACAO.matcher(normalizar(texto)).find()) {
            return Optional.of(RESPOSTA_MANIPULACAO);
        }
        return Optional.empty();
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}