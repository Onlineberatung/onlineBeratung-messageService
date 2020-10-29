package de.caritas.cob.messageservice.api.repository;

import de.caritas.cob.messageservice.api.model.entity.DraftMessage;
import org.springframework.data.repository.CrudRepository;

public interface DraftMessageRepository extends CrudRepository<DraftMessage, Long> {

}
