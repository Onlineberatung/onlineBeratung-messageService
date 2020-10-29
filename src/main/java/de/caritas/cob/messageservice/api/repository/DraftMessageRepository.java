package de.caritas.cob.messageservice.api.repository;

import de.caritas.cob.messageservice.api.model.draftmessage.entity.DraftMessage;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface DraftMessageRepository extends CrudRepository<DraftMessage, Long> {

  Optional<DraftMessage> findByUserIdAndRcGroupId(String userId, String rcGroupId);

}
