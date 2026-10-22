package br.com.wxnchatbot.backend.ferramenta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

class RelogioFalso extends Clock {

	private Instant agora = Instant.parse("2026-10-22T12:00:00Z");

	void avancar(Duration duracao) {
		agora = agora.plus(duracao);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return this;
	}

	@Override
	public Instant instant() {
		return agora;
	}
}
