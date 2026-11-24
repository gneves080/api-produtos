package com.exemplo.produtos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProdutoControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    /** Cria um produto pela própria API e devolve o id gerado (cada teste usa o seu, sem depender de ordem). */
    private int criarProduto(String nome, double preco) throws Exception {
        MvcResult resultado = mvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"preco\":" + preco + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode corpo = json.readTree(resultado.getResponse().getContentAsString());
        return corpo.get("id").asInt();
    }

    @Test
    void getListaTodosOsProdutos() throws Exception {
        mvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].nome").value("Notebook"));
    }

    @Test
    void getPorIdDevolveOProduto() throws Exception {
        mvc.perform(get("/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Notebook"))
                .andExpect(jsonPath("$.preco").value(3500.0));
    }

    @Test
    void getPorIdInexistenteDevolve404() throws Exception {
        mvc.perform(get("/produtos/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postCriaProdutoComIdGeradoPelaApi() throws Exception {
        // o id enviado (777) deve ser ignorado
        MvcResult resultado = mvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":777,\"nome\":\"Monitor\",\"preco\":899.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Monitor"))
                .andExpect(jsonPath("$.preco").value(899.0))
                .andReturn();

        int id = json.readTree(resultado.getResponse().getContentAsString()).get("id").asInt();
        assertNotEquals(777, id);
        assertEquals("/produtos/" + id, resultado.getResponse().getHeader("Location"));

        mvc.perform(get("/produtos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Monitor"));
    }

    @Test
    void postComDadosInvalidosDevolve400() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"preco\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem preço\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Negativo\",\"preco\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchAtualizaSoOsCamposEnviados() throws Exception {
        int id = criarProduto("Headset", 200.0);

        // só o preço
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":150.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Headset"))
                .andExpect(jsonPath("$.preco").value(150.0));

        // só o nome
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Headset Gamer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Headset Gamer"))
                .andExpect(jsonPath("$.preco").value(150.0));

        // um id enviado no corpo não altera o id do recurso
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":555,\"preco\":160.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mvc.perform(get("/produtos/" + id))
                .andExpect(jsonPath("$.nome").value("Headset Gamer"))
                .andExpect(jsonPath("$.preco").value(160.0));
    }

    @Test
    void patchInexistenteDevolve404EInvalidoDevolve400() throws Exception {
        mvc.perform(patch("/produtos/9999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":1}"))
                .andExpect(status().isNotFound());

        int id = criarProduto("Webcam", 300.0);
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":-5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());

        // nome válido + preço inválido: a requisição é recusada e NADA é alterado
        mvc.perform(patch("/produtos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro nome\",\"preco\":-5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/produtos/" + id))
                .andExpect(jsonPath("$.nome").value("Webcam"))
                .andExpect(jsonPath("$.preco").value(300.0));
    }

    @Test
    void deleteRemoveOProduto() throws Exception {
        int id = criarProduto("Cabo HDMI", 30.0);

        mvc.perform(delete("/produtos/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/produtos/" + id))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/produtos/" + id))
                .andExpect(status().isNotFound());
    }
}
