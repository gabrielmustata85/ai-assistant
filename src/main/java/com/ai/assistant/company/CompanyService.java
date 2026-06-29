package com.ai.assistant.company;

import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    public Company create(Company company) {
        company.setId(null);
        if (company.getVatPayer() == null) company.setVatPayer(false);
        return repository.save(company);
    }

    public Company get(Long id) {
        return repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
    }

    /** Aplică pe entitatea existentă doar câmpurile non-null din patch. */
    public Company update(Long id, Company patch) {
        Company existing = get(id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getCui() != null) existing.setCui(patch.getCui());
        if (patch.getCompanyType() != null) existing.setCompanyType(patch.getCompanyType());
        if (patch.getTaxRegime() != null) existing.setTaxRegime(patch.getTaxRegime());
        if (patch.getVatPayer() != null) existing.setVatPayer(patch.getVatPayer());
        return repository.save(existing);
    }
}
