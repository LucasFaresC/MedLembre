package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.List;

public class GerenciadorUsuario {

    private List<Usuario> listaUsuarios;
    private int proximoId;

    public GerenciadorUsuario() {
        this.listaUsuarios = new ArrayList<>();
        this.proximoId = 1;
    }

    // Cadastra um usuário novo (id automático) e salva nos CSV
    public boolean cadastrarUsuario(Usuario u) {
        if (u == null || u.getNome() == null || u.getNome().isEmpty()
                || u.getDataNascimento() == null
                || u.getGeneroBiologico() == null || u.getGeneroBiologico().isEmpty()) {
            return false;
        }
        u.setId(proximoId);
        proximoId++;
        listaUsuarios.add(u);
        salvar();
        return true;
    }

    // Devolve o usuário com esse id, ou null se não existir
    public Usuario buscarUsuario(int id) {
        for (Usuario u : listaUsuarios) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    // Troca o usuário antigo (mesmo id) pelo usuário editado
    public boolean editarUsuario(Usuario u) {
        for (int i = 0; i < listaUsuarios.size(); i++) {
            if (listaUsuarios.get(i).getId() == u.getId()) {
                listaUsuarios.set(i, u);
                salvar();
                return true;
            }
        }
        return false;
    }

    // Remove o usuário (e, com ele, seus contatos)
    public boolean deletarUsuario(int id) {
        Usuario u = buscarUsuario(id);
        if (u == null) {
            return false;
        }
        listaUsuarios.remove(u);
        salvar();
        return true;
    }

    // Devolve uma cópia da lista com todos os usuários (RF07)
    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(listaUsuarios);
    }

    // Lê usuarios.csv e contatos.csv (a App chama uma vez, quando o programa abre)
    public void carregar() {
        listaUsuarios = CsvUtil.carregarUsuarios();
        CsvUtil.carregarContatos(listaUsuarios);
        proximoId = 1;
        for (Usuario u : listaUsuarios) {
            if (u.getId() >= proximoId) {
                proximoId = u.getId() + 1;
            }
        }
    }

    // Grava usuarios.csv e contatos.csv
    private void salvar() {
        CsvUtil.salvarUsuarios(listaUsuarios);
        CsvUtil.salvarContatos(listaUsuarios);
    }
}