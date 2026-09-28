package com.dasifind.backend.domain.policeitem.repository;

import com.dasifind.backend.domain.policeitem.entity.PoliceItem;
import com.dasifind.backend.domain.policeitem.model.PoliceItemSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PoliceItemRepository extends JpaRepository<PoliceItem, Long> {
    Optional<PoliceItem> findBySourceAndManagementNoAndItemSequence(
            PoliceItemSource source, String managementNo, int itemSequence);
}
