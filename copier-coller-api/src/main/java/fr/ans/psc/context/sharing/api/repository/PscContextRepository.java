package fr.ans.psc.context.sharing.api.repository;

import fr.ans.psc.context.sharing.api.model.PscContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PscContextRepository extends CrudRepository<PscContext, String> {
}
