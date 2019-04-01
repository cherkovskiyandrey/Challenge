package com.tango;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tango.domain.AggregatedStat;
import com.tango.domain.Stat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StatControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void simpleSuccessScenarioTest() throws Exception {
        mvc.perform(post("/transactions")
                .content(objectMapper.writeValueAsString(new Stat(12.4, System.currentTimeMillis())))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AggregatedStat(12.4, 12.4, 12.4, 12.4, 1)
                )));
    }

    @Test
    public void invalidDataTest() throws Exception {
        mvc.perform(post("/transactions")
                .content(objectMapper.writeValueAsString(new Stat(-12.4, System.currentTimeMillis())))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        mvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AggregatedStat(0, 0, 0, 0, 0)
                )));
    }

    @Test
    public void rottedTest() throws Exception {
        mvc.perform(post("/transactions")
                .content(objectMapper.writeValueAsString(new Stat(12.4, System.currentTimeMillis())))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AggregatedStat(12.4, 12.4, 12.4, 12.4, 1)
                )));

        TimeUnit.SECONDS.sleep(7);
        mvc.perform(get("/statistics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(
                        new AggregatedStat(0, 0, 0, 0, 0)
                )));
    }
}