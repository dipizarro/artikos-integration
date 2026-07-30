package cl.atk.nomina.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArtikosSourcePropertiesTest {

    @Test
    void defaultsToRemoteMode() {
        ArtikosSourceProperties properties = new ArtikosSourceProperties();

        assertThat(properties.getMode()).isEqualTo("remote");
        assertThat(properties.isRemoteMode()).isTrue();
        assertThat(properties.isLocalXmlMode()).isFalse();
    }
}
