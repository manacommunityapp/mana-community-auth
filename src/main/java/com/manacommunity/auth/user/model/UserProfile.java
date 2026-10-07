package com.manacommunity.auth.user.model;

import com.manacommunity.common.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(length = 1000)
    private String bio;

    @Column(name = "cover_pic_url", length = 255)
    private String coverPicUrl;

    @Column(length = 1000)
    private String skills; // Stored as comma-separated values (e.g. "Cricket, Badminton")

    @Column(name = "posts_count")
    @Builder.Default
    private Integer posts = 0;

    @Column(name = "connections_count")
    @Builder.Default
    private Integer connections = 0;

    @Column(name = "events_attended_count")
    @Builder.Default
    private Integer eventsAttended = 0;

    @Column(name = "items_sold_count")
    @Builder.Default
    private Integer itemsSold = 0;

    @Column(name = "jobs_posted_count")
    @Builder.Default
    private Integer jobsPosted = 0;

    @Column(name = "sports_played_count")
    @Builder.Default
    private Integer sportsPlayed = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (posts == null) posts = 0;
        if (connections == null) connections = 0;
        if (eventsAttended == null) eventsAttended = 0;
        if (itemsSold == null) itemsSold = 0;
        if (jobsPosted == null) jobsPosted = 0;
        if (sportsPlayed == null) sportsPlayed = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
