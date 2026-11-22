package br.com.wxnchatbot.backend.chatbot;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class FiltroDeEntrada {

    static final int TAMANHO_MAXIMO = 1000;

    public static final String RESPOSTA_MUITO_LONGA = "Sua mensagem ficou muito longa. Pode resumir em poucas linhas o que você precisa?";
    public static final String RESPOSTA_MANIPULACAO = "Não consigo mudar a forma como eu atendo. Posso ajudar com o status de demandas, a abertura de chamados ou chamar um atendente.";

    private static final Pattern MANIPULACAO = Pattern.compile(
            "\\b(ignore|ignora|ignorar|esqueca|esquece|esquecer|desconsidere|desconsidera)\\b.{0,30}\\b(instrucoes|instrucao|regras|ordens|prompt)\\b"
                    + "|\\b(revele|revela|mostre|mostra|repita|repete|diga|me passe)\\b.{0,30}\\b(prompt|instrucoes)\\b"
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