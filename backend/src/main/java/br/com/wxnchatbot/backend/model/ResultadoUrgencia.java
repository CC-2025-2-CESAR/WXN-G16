package br.com.wxnchatbot.backend.model;

public class ResultadoUrgencia {
    private NivelUrgencia nivel;
    private String termo;

    public ResultadoUrgencia (NivelUrgencia nivel, String termo){
        this.nivel = nivel;
        this.termo = termo;
    }

    public NivelUrgencia getNivel(){
        return nivel;
    }

    public String getTermo (){
        return termo;
    }
}
