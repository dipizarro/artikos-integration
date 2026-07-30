package cl.atk.nomina.batch.procurement.lookup;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementMappingLookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcurementMappingLookupService.class);
    private static final String ACTIVE = "V";

    private final GrlMaeItemDetRepository detailRepository;
    private final GrlMaeItemRepository itemRepository;

    public ProcurementMappingLookupService(
            GrlMaeItemDetRepository detailRepository,
            GrlMaeItemRepository itemRepository) {
        this.detailRepository = detailRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public ProcurementItemLookupResult resolveItemForDistribution(
            String codEmpres,
            String codSistem,
            Long codCuenta,
            String codImpsto) {
        String normalizedCodEmpres = normalize(codEmpres);
        String normalizedCodSistem = normalize(codSistem);
        String normalizedCodImpsto = normalize(codImpsto);
        List<GrlMaeItemDetEntity> details = detailRepository.findActiveMappingsByAccount(
                normalizedCodEmpres, codCuenta, normalizedCodSistem, normalizedCodImpsto, ACTIVE);
        LOGGER.info(
                "ASI GRL_MAE_ITEM_DET lookup codEmpres={} codCuenta={} codSistem={} codImpsto={} numPeriodo={} resultCount={}",
                normalizedCodEmpres,
                codCuenta,
                normalizedCodSistem,
                normalizedCodImpsto,
                resolvedNumPeriodo(details),
                details.size());
        if (details.isEmpty()) {
            List<GrlMaeItemDetEntity> candidates =
                    detailRepository.findActiveMappingsByAccount(normalizedCodEmpres, codCuenta, ACTIVE);
            throw new ProcurementMappingException(
                    "No ASI item mapping found in GRL_MAE_ITEM_DET for codEmpres=%s codCuenta=%s codSistem=%s codImpsto=%s availableMappings=%s"
                            .formatted(
                                    normalizedCodEmpres,
                                    codCuenta,
                                    normalizedCodSistem,
                                    normalizedCodImpsto,
                                    availableMappings(candidates)));
        }
        if (details.size() > 1) {
            throw new ProcurementMappingException(
                    "Ambiguous ASI item mapping in GRL_MAE_ITEM_DET for codEmpres=%s codCuenta=%s codSistem=%s codImpsto=%s resultCount=%d mappings=%s"
                            .formatted(
                                    normalizedCodEmpres,
                                    codCuenta,
                                    normalizedCodSistem,
                                    normalizedCodImpsto,
                                    details.size(),
                                    availableMappings(details)));
        }

        GrlMaeItemDetEntity detail = details.get(0);
        String grlCodItem = normalize(detail.getId().getGrlCodItem());
        String detailCodEmpres = normalize(detail.getId().getCodEmpres());
        Integer detailNumPeriodo = detail.getId().getNumPeriodo();
        boolean itemExists = itemRepository.existsActiveItem(
                detailCodEmpres, detailNumPeriodo, grlCodItem, ACTIVE);
        if (!itemExists) {
            throw new ProcurementMappingException(
                    "ASI item mapping detail exists but master GRL_MAE_ITEM is not active for codEmpres=%s numPeriodo=%s grlCodItem=%s"
                            .formatted(detailCodEmpres, detailNumPeriodo, grlCodItem));
        }

        return new ProcurementItemLookupResult(
                grlCodItem,
                normalize(detail.getCodTipUnid()),
                normalize(detail.getCodTipCntaItems()),
                normalize(detail.getCodContbl()),
                normalize(detail.getId().getCodSistem()),
                detail.getId().getNumPeriodo(),
                normalize(detail.getId().getCodImpsto()),
                normalize(detail.getId().getCodMoneda()),
                detail.getId().getCodCuenta());
    }

    private String availableMappings(List<GrlMaeItemDetEntity> candidates) {
        if (candidates.isEmpty()) {
            return "[]";
        }
        return candidates.stream()
                .map(detail -> "%s/%s/%s/%s/%s/%s".formatted(
                        normalize(detail.getId().getCodEmpres()),
                        detail.getId().getNumPeriodo(),
                        normalize(detail.getId().getCodSistem()),
                        normalize(detail.getId().getCodImpsto()),
                        normalize(detail.getId().getCodMoneda()),
                        detail.getId().getGrlCodItem()))
                .distinct()
                .limit(10)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private Integer resolvedNumPeriodo(List<GrlMaeItemDetEntity> details) {
        return details.stream()
                .map(detail -> detail.getId().getNumPeriodo())
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
