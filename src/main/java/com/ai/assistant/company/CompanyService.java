package com.ai.assistant.company;

import com.ai.assistant.auth.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository repository;
    private final CompanyAccessGuard accessGuard;

    public CompanyService(CompanyRepository repository, CompanyAccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    public Company create(Company company) {
        company.setId(null);
        company.setOwnerUserId(CurrentUser.id());   // firma aparține userului care o creează
        if (company.getVatPayer() == null) company.setVatPayer(false);
        return repository.save(company);
    }

    /** Firmele deținute de userul autentificat. */
    public java.util.List<Company> listMine() {
        return repository.findByOwnerUserId(CurrentUser.id());
    }

    /** PUNCT UNIC DE CONTROL: orice acces la o firmă trece prin verificarea de ownership. */
    public Company get(Long id) {
        Company company = repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        accessGuard.assertOwner(company.getOwnerUserId());
        return company;
    }

    public Company update(Long id, Company patch) {
        Company existing = get(id);   // get() verifică deja ownership-ul
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getCui() != null) existing.setCui(patch.getCui());
        if (patch.getCompanyType() != null) existing.setCompanyType(patch.getCompanyType());
        if (patch.getTaxRegime() != null) existing.setTaxRegime(patch.getTaxRegime());
        if (patch.getVatPayer() != null) existing.setVatPayer(patch.getVatPayer());
        if (patch.getRegCom() != null) existing.setRegCom(patch.getRegCom());
        if (patch.getAddress() != null) existing.setAddress(patch.getAddress());
        if (patch.getIban() != null) existing.setIban(patch.getIban());
        if (patch.getBank() != null) existing.setBank(patch.getBank());
        if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        return repository.save(existing);
    }
}
