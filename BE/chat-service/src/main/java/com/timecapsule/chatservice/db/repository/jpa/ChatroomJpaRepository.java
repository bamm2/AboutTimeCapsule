package com.timecapsule.chatservice.db.repository.jpa;

import com.timecapsule.chatservice.db.entity.Chatroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatroomJpaRepository extends JpaRepository<Chatroom, String> {
}
