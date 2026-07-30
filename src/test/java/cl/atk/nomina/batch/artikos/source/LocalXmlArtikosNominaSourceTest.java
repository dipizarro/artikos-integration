package cl.atk.nomina.batch.artikos.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

class LocalXmlArtikosNominaSourceTest {

    @Test
    void readsLocalXmlOnce() {
        LocalXmlArtikosNominaSource source = source("classpath:samples/artikos/ZSVIDA_Nom15960.xml");

        Optional<Nomina> first = source.fetchNextNomina(ArtikosProfileType.VIDA);
        Optional<Nomina> second = source.fetchNextNomina(ArtikosProfileType.VIDA);

        assertThat(first).isPresent();
        assertThat(first.get().cabecera().numeroNomina()).isEqualTo(15960L);
        assertThat(second).isEmpty();
    }

    @Test
    void throwsClearErrorWhenLocalXmlDoesNotExist() {
        LocalXmlArtikosNominaSource source = source("classpath:samples/artikos/missing.xml");

        assertThatThrownBy(() -> source.fetchNextNomina(ArtikosProfileType.VIDA))
                .isInstanceOf(NominaXmlParsingException.class)
                .hasMessageContaining("No existe XML local Artikos");
    }

    private LocalXmlArtikosNominaSource source(String path) {
        ArtikosSourceProperties properties = new ArtikosSourceProperties();
        properties.setMode(ArtikosSourceProperties.MODE_LOCAL_XML);
        properties.setLocalXmlPath(path);
        return new LocalXmlArtikosNominaSource(
                properties,
                new DefaultResourceLoader(),
                new NominaXmlParserService(new ClassPathResource("samples/ZSVIDA_Nom15960.xml")));
    }
}
