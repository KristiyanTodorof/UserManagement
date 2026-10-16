package com.acmestack.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.conversation.id = :cid ORDER BY m.id DESC")
    List<Message> findLatest(@Param("cid") Long cid, Pageable pageable);

    @Query("""
        SELECT m FROM Message m JOIN FETCH m.sender
        WHERE m.conversation.id = :cid AND m.id > :after ORDER BY m.id ASC
        """)
    List<Message> findAfter(@Param("cid") Long cid, @Param("after") long after, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE m.conversation.id = :cid AND m.attachmentName IS NOT NULL ORDER BY m.id DESC
        """)
    List<Message> findAttachments(@Param("cid") Long cid, Pageable pageable);

    @Query("SELECT m FROM Message m JOIN FETCH m.conversation WHERE m.id = :id")
    Optional<Message> findWithConversation(@Param("id") Long id);

    @Query("""
        SELECT new com.acmestack.chat.ConvCount(m.conversation.id, COUNT(m))
        FROM Message m, ConversationMember cm
        WHERE cm.user.id = :uid AND cm.conversation.id = m.conversation.id
          AND m.id > cm.lastReadMessageId AND m.sender.id <> :uid
        GROUP BY m.conversation.id
        """)
    List<ConvCount> countUnread(@Param("uid") Long uid);
}