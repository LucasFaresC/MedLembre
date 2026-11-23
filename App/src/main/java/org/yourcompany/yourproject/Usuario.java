/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.yourcompany.yourproject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author rocha
 */

public class Usuario {
   private int id;
    private String nome;
    private LocalDate dataNascimento;
    private String fotoPath;
    private String generoBiologico;
    private List<ContatoEmergencia> contatos;



    
    public Usuario() {
        this.contatos = new ArrayList<>();
    }

    
    public Usuario(int id, String nome, LocalDate dataNascimento, String fotoPath, String generoBiologico) {
        this();
        this.id = id;
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.fotoPath = fotoPath;
        this.generoBiologico = generoBiologico;
    }

        
    public boolean adicionarContato(ContatoEmergencia c) {
        if (c == null || this.contatos.size() >= 3) {
            return false;
        }
        this.contatos.add(c);
        return true;
    }

    public void removerContato(int id) {
        this.contatos.removeIf(contato -> contato.getId() == id);
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

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getFotoPath() {
        return fotoPath;
    }

    public void setFotoPath(String fotoPath) {
        this.fotoPath = fotoPath;
    }

    public String getGeneroBiologico() {
        return generoBiologico;
    }

    public void setGeneroBiologico(String generoBiologico) {
        this.generoBiologico = generoBiologico;
    }

    public List<ContatoEmergencia> getContatos() {
        return contatos;
    }

    public void setContatos(List<ContatoEmergencia> contatos) {
        this.contatos = contatos;
    }

   
}
