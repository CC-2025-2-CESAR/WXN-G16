package br.com.wxnchatbot.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.repository.AtendenteRepositorio;

@Service
public class AtendenteServico {

	private final AtendenteRepositorio atendenteRepositorio;

	public AtendenteServico(AtendenteRepositorio atendenteRepositorio) {
		this.atendenteRepositorio = atendenteRepositorio;
	}

	@Transactional(readOnly = true)
	public List<AtendenteResumo> listar() {
		return atendenteRepositorio.findAll(Sort.by("nome")).stream().map(AtendenteResumo::de).toList();
	}
}
