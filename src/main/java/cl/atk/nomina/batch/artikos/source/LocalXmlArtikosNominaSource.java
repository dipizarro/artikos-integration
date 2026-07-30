package cl.atk.nomina.batch.artikos.source;

import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Component
@StepScope
@ConditionalOnProperty(name = "artikos.source.mode", havingValue = "local-xml")
public class LocalXmlArtikosNominaSource implements ArtikosNominaSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalXmlArtikosNominaSource.class);

    private final ArtikosSourceProperties sourceProperties;
    private final ResourceLoader resourceLoader;
    private final NominaXmlParserService parserService;
    private boolean consumed;

    public LocalXmlArtikosNominaSource(
            ArtikosSourceProperties sourceProperties,
            ResourceLoader resourceLoader,
            NominaXmlParserService parserService) {
        this.sourceProperties = sourceProperties;
        this.resourceLoader = resourceLoader;
        this.parserService = parserService;
    }

    @Override
    public Optional<Nomina> fetchNextNomina(ArtikosProfileType profile) {
        if (consumed) {
            LOGGER.info("Local XML Artikos source already consumed profile={} path={}",
                    profile, sourceProperties.getLocalXmlPath());
            return Optional.empty();
        }
        consumed = true;

        String localXmlPath = sourceProperties.getLocalXmlPath();
        if (!StringUtils.hasText(localXmlPath)) {
            throw new NominaXmlParsingException("artikos.source.local-xml-path es obligatorio en modo local-xml");
        }

        try {
            Resource resource = resourceLoader.getResource(localXmlPath);
            if (!resource.exists()) {
                throw new NominaXmlParsingException("No existe XML local Artikos: " + localXmlPath);
            }
            String rawXml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            Nomina nomina = parserService.parseFromString(rawXml);
            LOGGER.info("Local XML Artikos nomina loaded profile={} path={} numeroNomina={} documentos={}",
                    profile,
                    localXmlPath,
                    nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina(),
                    nomina.documentos() == null ? 0 : nomina.documentos().size());
            return Optional.of(nomina);
        } catch (IOException exception) {
            throw new NominaXmlParsingException("No fue posible leer XML local Artikos: " + localXmlPath, exception);
        }
    }
}
