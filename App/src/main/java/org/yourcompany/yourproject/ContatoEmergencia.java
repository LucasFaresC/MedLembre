/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.yourcompany.yourproject;

public class ContatoEmergencia {

    private int id;
    private String nome;
    private String telefone;
    private String relacao;

    public ContatoEmergencia() {
    }

    public ContatoEmergencia(int id, String nome, String telefone, String relacao) {
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.relacao = relacao;
    }

    // Simula o envio de uma mensagem de emergência para este contato (RF12)
    public void enviarNotificacao() {
        System.out.println("Mensagem enviada para " + nome + " (" + telefone + "): "+ "Preciso de ajuda!");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getRelacao() {
        return relacao;
    }

    public void setRelacao(String relacao) {
        this.relacao = relacao;
    }
}


