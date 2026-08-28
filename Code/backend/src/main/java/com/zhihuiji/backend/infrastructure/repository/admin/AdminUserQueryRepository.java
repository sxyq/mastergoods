package com.zhihuiji.backend.infrastructure.repository.admin;

import com.zhihuiji.backend.domain.entity.UserEntity;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Owner/store-bound user projection source for administrator reads. */
public interface AdminUserQueryRepository extends Repository<UserEntity, Long> {
    @Query(value = """
        select u
          from UserEntity u
         where (
             :allOwners = true
             or u.id in :ownerUserIds
             or exists (
                 select m.id
                   from StoreMembershipEntity m
                  where m.userId = u.id
                    and m.ownerUserId in :ownerUserIds
                    and (:allStores = true or m.storeId in :storeIds)
             )
         )
           and (
             :keyword is null or :keyword = ''
             or lower(u.phone) like lower(concat('%', :keyword, '%'))
             or lower(u.nickname) like lower(concat('%', :keyword, '%'))
           )
         order by u.updatedAt desc, u.id desc
        """,
        countQuery = """
        select count(u.id)
          from UserEntity u
         where (
             :allOwners = true
             or u.id in :ownerUserIds
             or exists (
                 select m.id
                   from StoreMembershipEntity m
                  where m.userId = u.id
                    and m.ownerUserId in :ownerUserIds
                    and (:allStores = true or m.storeId in :storeIds)
             )
         )
           and (
             :keyword is null or :keyword = ''
             or lower(u.phone) like lower(concat('%', :keyword, '%'))
             or lower(u.nickname) like lower(concat('%', :keyword, '%'))
           )
        """)
    Page<UserEntity> findUsers(
        @Param("allOwners") boolean allOwners,
        @Param("ownerUserIds") Collection<Long> ownerUserIds,
        @Param("storeIds") Collection<Long> storeIds,
        @Param("allStores") boolean allStores,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
