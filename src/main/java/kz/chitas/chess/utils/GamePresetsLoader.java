package kz.chitas.chess.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kz.chitas.chess.model.logic.GameType;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class GamePresetsLoader {
    public Map<String, GameType> loadPresets() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("game-presets.json")) {
            if (in == null) {
                throw new IllegalStateException("Config file not found: game-presets.json");
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            List<GameType> presets = mapper.readValue(json, new TypeReference<List<GameType>>() {
            });
            log.info("Presets Loading Completed");
            return presets.stream()
                    .collect(Collectors.toMap(GameType::getName, obj -> obj));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load presets", e);
        }
    }

}
