package com.hyeonpyo.wallpadcontroller.domain.definition.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "packet_field")
@Getter
public class PacketField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packet_type_id")
    private PacketType packetType;

    private Integer position;

    private String name;

    @OneToMany(mappedBy = "packetField", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PacketFieldValue> valueMappings = new LinkedHashSet<>();
}