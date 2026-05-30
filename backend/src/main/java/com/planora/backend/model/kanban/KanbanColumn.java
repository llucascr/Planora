package com.planora.backend.model.kanban;

import com.planora.backend.model.issue.Issue;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "kanban_column")
public class KanbanColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kanban_column_id")
    private Long kanbanColumnId;

    private String name;
    private Integer position;
    private Boolean isColumnDone;

    @ManyToOne
    @JoinColumn(name = "kanban_board_id")
    private KanbanBoard kanbanBoard;

    @OneToMany(mappedBy = "column", cascade =  CascadeType.ALL)
    private List<Issue> issues;

}
