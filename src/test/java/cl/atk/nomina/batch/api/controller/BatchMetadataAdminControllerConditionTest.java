package cl.atk.nomina.batch.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cl.atk.nomina.batch.service.BatchMetadataPurgeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BatchMetadataAdminControllerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(BatchMetadataPurgeService.class, () -> mock(BatchMetadataPurgeService.class))
            .withUserConfiguration(BatchMetadataAdminController.class);

    @Test
    void doesNotLoadAdminControllerWhenAdminModeIsDisabled() {
        contextRunner
                .withPropertyValues("app.admin.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(BatchMetadataAdminController.class));
    }

    @Test
    void loadsAdminControllerWhenAdminModeIsEnabled() {
        contextRunner
                .withPropertyValues("app.admin.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(BatchMetadataAdminController.class));
    }
}
