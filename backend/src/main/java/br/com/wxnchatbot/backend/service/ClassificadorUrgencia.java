package br.com.wxnchatbot.backend.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.ResultadoUrgencia;

@Component
public class ClassificadorUrgencia {
    private static final String[] GATILHOS_ALTA = {
            "urgente", "urgencia", "emergencia",
            "fora do ar", "sistema parado", "sistema parou", "sistema caiu",
            "tudo parado", "nada funciona",
            "nao consigo emitir nota", "nao consigo faturar", "nao consigo vender",
            "perdendo vendas", "prejuizo",
            "cobrado duas vezes", "cobranca duplicada", "vazamento de dados", "dados vazados"
    };

    private static final String[] GATILHOS_MEDIA = {
            "erro", "erros", "falha",
            "problema", "problemas", "travou", "lento", "lenta", "lentos", "lentas", "lentidao",
            "nao funciona", "nao consigo", "nao abre", "reclamacao", "ainda nao resolveram"
    };

    private static final String[] NEGACOES = { "nao", "sem", "nada", "nem", "nenhuma", "nenhum"};

    private static final String[] JA_RESOLVIDO = { "ja voltou", "ja normalizou", "ja foi normalizado", "ja resolvido", "ja resolveram", "voltou ao normal", "normalizou"};

    public ResultadoUrgencia classificar(String mensagem){
        if (mensagem == null || mensagem.isBlank()) {
            return new ResultadoUrgencia(NivelUrgencia.BAIXA, null);
        }

        String texto = normalizar(mensagem);

        // Se o problema já passou, ignoramos os GATILHO_ALTA.

        /*
        Os GATILHO_MEDIA continuam valendo de propósito: se a mensagem cita um erro
		e algo "já resolvido", não dá para saber qual ainda está aberto, e é
		melhor errar para cima do que esconder um problema em aberto.
         */

        if (!contemAlguma(texto, JA_RESOLVIDO)){
            String termoAlta = procurarGatilho(texto, GATILHOS_ALTA);
            if (termoAlta != null){
                return new ResultadoUrgencia(NivelUrgencia.ALTA, termoAlta);
            }
        }

        String termoMedia = procurarGatilho(texto, GATILHOS_MEDIA);
        if (termoMedia != null){
            return new ResultadoUrgencia(NivelUrgencia.MEDIA, termoMedia);
        }

        return new ResultadoUrgencia(NivelUrgencia.BAIXA,null);
    }

    // Deixa o texto em minúsculas, sem acento, sem pontuação e com um espaço
    private String normalizar(String mensagem) {
        String texto = mensagem.toLowerCase();
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("\\p{M}", ""); // remove os acentos
        texto = texto.replaceAll("[^a-z0-9 ]", " "); // troca pontuação por espaço
        texto = texto.replaceAll(" +", " ").trim(); // junta espaços repetidos
        return " " + texto + " ";
    }

    //Vai procurar o primeiro gatilho que não esteja negado
    private String procurarGatilho(String texto, String[] gatilhos) {
        for (String gatilho : gatilhos) {
            String busca = " " + gatilho + " ";
            int posicao = texto.indexOf(busca);

            while (posicao != -1) {
                if (!temNegacaoAntes(texto, posicao)) {
                    return gatilho;
                }
                posicao = texto.indexOf(busca, posicao + 1);
            }
        }
        return null;
    }

    // Olha as 3 palavras que vêm antes de "posicao" e vê se alguma é uma negação.
    private boolean temNegacaoAntes(String texto, int posicao) {
        String antes = texto.substring(0, posicao).trim();
        String[] palavras = antes.split(" ");

        int inicio = Math.max(0, palavras.length - 3);
        for (int i = inicio; i < palavras.length; i++) {
            for (String negacao : NEGACOES) {
                if (palavras[i].equals(negacao)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contemAlguma(String texto, String[] frases) {
        for (String frase : frases) {
            if (texto.contains(" " + frase + " ")) {
                return true;
            }
        }
        return false;
    }
}
