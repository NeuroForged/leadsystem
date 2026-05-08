package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.MeetingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Long> {
    boolean existsByFirefliesId(String firefliesId);
    List<MeetingNote> findByMeeting_Id(Long meetingId);
}
