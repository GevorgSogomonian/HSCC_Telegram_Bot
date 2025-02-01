package org.example.data_classes.data_base.mysql_sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UniqueNumberSeq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long number;
}
