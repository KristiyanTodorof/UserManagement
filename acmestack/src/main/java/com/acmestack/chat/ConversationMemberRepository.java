package com.acmestack.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    @Query("""
        SELECT cm FROM ConversationMember cm JOIN FETCH cm.conversation c
        WHERE cm.user.id = :uid
        ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC, c.id DESC
        """)
    List<ConversationMember> findForUser(@Param("uid") Long uid);

    @Query("""
        SELECT cm FROM ConversationMember cm JOIN FETCH cm.conversation
        WHERE cm.conversation.id = :cid AND cm.user.id = :uid
        """)
    Optional<ConversationMember> findMembership(@Param("cid") Long cid, @Param("uid") Long uid);

    @Query("""
        SELECT cm FROM ConversationMember cm JOIN FETCH cm.user u JOIN FETCH u.role
        WHERE cm.conversation.id = :cid ORDER BY u.name
        """)
    List<ConversationMember> findMembers(@Param("cid") Long cid);

    @Query("""
        SELECT cm FROM ConversationMember cm JOIN FETCH cm.user u JOIN FETCH u.role
        WHERE cm.conversation.id IN :ids
          AND cm.conversation.type = com.acmestack.chat.ConversationType.DIRECT
          AND u.id <> :uid
        """)
    List<ConversationMember> findOthersInDirects(@Param("ids") List<Long> ids, @Param("uid") Long uid);

    @Modifying
    @Query("""
        UPDATE ConversationMember cm SET cm.lastReadMessageId = :msgId
        WHERE cm.conversation.id = :cid AND cm.user.id = :uid AND cm.lastReadMessageId < :msgId
        """)
    int markRead(@Param("cid") Long cid, @Param("uid") Long uid, @Param("msgId") long msgId);
}