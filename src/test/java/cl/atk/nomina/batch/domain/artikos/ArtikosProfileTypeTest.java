package cl.atk.nomina.batch.domain.artikos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArtikosProfileTypeTest {

    @Test
    void parsesProfileIgnoringCase() {
        assertThat(ArtikosProfileType.from("vida")).isEqualTo(ArtikosProfileType.VIDA);
        assertThat(ArtikosProfileType.from("GENERALES")).isEqualTo(ArtikosProfileType.GENERALES);
    }

    @Test
    void rejectsUnknownProfile() {
        assertThatThrownBy(() -> ArtikosProfileType.from("OTRO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil Artikos no soportado");
    }
}
