package com.KairoLink.repository;

import com.KairoLink.entity.IssueReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {

    @Query("""
        select report from IssueReport report
        join fetch report.reporter
        left join fetch report.ride
        left join fetch report.booking
        order by report.createdAt desc
        """)
    List<IssueReport> findAllWithDetails();

    @Query("""
        select report from IssueReport report
        join fetch report.reporter
        left join fetch report.ride
        left join fetch report.booking
        where report.reporter.id = :reporterId
        order by report.createdAt desc
        """)
    List<IssueReport> findByReporterIdWithDetails(@Param("reporterId") Long reporterId);
}
