package cl.atk.nomina.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artikos")
public class ArtikosOutboundProperties {

    private Toggle confirm = new Toggle();
    private Toggle result = new Toggle();

    public Toggle getConfirm() {
        return confirm;
    }

    public void setConfirm(Toggle confirm) {
        this.confirm = confirm == null ? new Toggle() : confirm;
    }

    public Toggle getResult() {
        return result;
    }

    public void setResult(Toggle result) {
        this.result = result == null ? new Toggle() : result;
    }

    public boolean isConfirmEnabled() {
        return confirm.isEnabled();
    }

    public boolean isResultEnabled() {
        return result.isEnabled();
    }

    public static class Toggle {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
