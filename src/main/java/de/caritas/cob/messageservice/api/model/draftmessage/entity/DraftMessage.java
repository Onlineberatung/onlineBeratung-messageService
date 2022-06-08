package de.caritas.cob.messageservice.api.model.draftmessage.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draftmessage")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DraftMessage {

  @Id
  @SequenceGenerator(name = "id_seq", allocationSize = 1, sequenceName = "sequence_draftmessage")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "rc_group_id", nullable = false)
  private String rcGroupId;

  @Column(name = "draft_message", nullable = false)
  private String message;

  @Column(name = "create_date", nullable = false)
  private LocalDateTime createDate;

  @Column(name = "t")
  private String t;

  @Column(name = "org")
  private String org;
}
