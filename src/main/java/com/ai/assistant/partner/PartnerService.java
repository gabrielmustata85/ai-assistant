package com.ai.assistant.partner;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartnerService {

    private final PartnerRepository repository;
    private final CompanyService companyService;

    public PartnerService(PartnerRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    public List<Partner> list(Long companyId) {
        companyService.get(companyId);   // verifică ownership
        return repository.findByCompanyIdOrderByNameAsc(companyId);
    }

    public Partner add(Long companyId, Partner partner) {
        companyService.get(companyId);
        partner.setId(null);
        partner.setCompanyId(companyId);
        return repository.save(partner);
    }

    public Partner update(Long id, Partner data) {
        Partner p = repository.findById(id).orElseThrow();
        companyService.get(p.getCompanyId());   // ownership
        p.setName(blankToKeep(data.getName(), p.getName()));
        p.setCui(data.getCui());
        p.setIban(data.getIban());
        p.setPhone(data.getPhone());
        p.setEmail(data.getEmail());
        p.setAddress(data.getAddress());
        return repository.save(p);
    }

    public void delete(Long id) {
        Partner p = repository.findById(id).orElseThrow();
        companyService.get(p.getCompanyId());   // ownership
        repository.deleteById(id);
    }

    /**
     * Creează sau completează un colaborator din datele unei facturi. NU re-verifică ownership-ul
     * (apelantul deja l-a verificat). Face match după CUI, altfel după nume; completează doar
     * câmpurile lipsă, ca să nu suprascrie date bune cu goluri.
     */
    public void upsertFromInvoice(Long companyId, String name, String cui,
                                  String iban, String phone, String email, String address) {
        if (isBlank(name) && isBlank(cui)) return;   // nimic util de salvat

        Partner existing = null;
        if (!isBlank(cui)) {
            existing = repository.findFirstByCompanyIdAndCuiIgnoreCase(companyId, cui.trim()).orElse(null);
        }
        if (existing == null && !isBlank(name)) {
            existing = repository.findFirstByCompanyIdAndNameIgnoreCase(companyId, name.trim()).orElse(null);
        }

        if (existing == null) {
            Partner p = new Partner();
            p.setCompanyId(companyId);
            p.setName(isBlank(name) ? cui.trim() : name.trim());
            p.setCui(trimOrNull(cui));
            p.setIban(trimOrNull(iban));
            p.setPhone(trimOrNull(phone));
            p.setEmail(trimOrNull(email));
            p.setAddress(trimOrNull(address));
            repository.save(p);
        } else {
            boolean changed = false;
            if (isBlank(existing.getCui()) && !isBlank(cui)) { existing.setCui(cui.trim()); changed = true; }
            if (isBlank(existing.getIban()) && !isBlank(iban)) { existing.setIban(iban.trim()); changed = true; }
            if (isBlank(existing.getPhone()) && !isBlank(phone)) { existing.setPhone(phone.trim()); changed = true; }
            if (isBlank(existing.getEmail()) && !isBlank(email)) { existing.setEmail(email.trim()); changed = true; }
            if (isBlank(existing.getAddress()) && !isBlank(address)) { existing.setAddress(address.trim()); changed = true; }
            if (changed) repository.save(existing);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String blankToKeep(String candidate, String current) {
        return isBlank(candidate) ? current : candidate;
    }
}
