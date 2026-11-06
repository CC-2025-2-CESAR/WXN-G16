package br.com.wxnchatbot.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Telefones brasileiros do jeito que o WhatsApp os entrega (55 + DDD + número, às vezes sem o nono dígito) e do jeito
 * que ficam no cadastro (DDD + número).
 */
final class TelefoneBr {

	private static final String DDI = "55";

	private TelefoneBr() {
	}

	/** Só os dígitos, sem o 55 do país quando ele está presente. */
	static String nacional(String telefone) {
		String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
		boolean comPais = digitos.startsWith(DDI) && (digitos.length() == 12 || digitos.length() == 13);
		return comPais ? digitos.substring(DDI.length()) : digitos;
	}

	/** As formas em que o mesmo número pode estar no cadastro: com e sem o 55, com e sem o nono dígito. */
	static List<String> variantes(String telefone) {
		String nacional = nacional(telefone);
		Set<String> formas = new LinkedHashSet<>();
		formas.add(nacional);
		if (nacional.length() == 10) {
			formas.add(nacional.substring(0, 2) + "9" + nacional.substring(2));
		} else if (nacional.length() == 11 && nacional.charAt(2) == '9') {
			formas.add(nacional.substring(0, 2) + nacional.substring(3));
		}
		Set<String> comPais = new LinkedHashSet<>();
		formas.forEach(forma -> comPais.add(DDI + forma));
		formas.addAll(comPais);
		return List.copyOf(formas);
	}
}
