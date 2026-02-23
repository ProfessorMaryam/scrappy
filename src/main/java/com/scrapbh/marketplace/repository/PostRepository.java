package com.scrapbh.marketplace.repository;

import com.scrapbh.marketplace.entity.Post;
import com.scrapbh.marketplace.enums.PostStatus;
import com.scrapbh.marketplace.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    /**
     * Find all posts by user ID
     * @param userId the user ID to search for
     * @return List of posts belonging to the user
     */
    List<Post> findByUserId(UUID userId);
    
    /**
     * Find all posts by status
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts with the specified status
     */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    
    /**
     * Find all posts by post type and status
     * @param postType the post type to filter by
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts matching the criteria
     */
    Page<Post> findByPostTypeAndStatus(PostType postType, PostStatus status, Pageable pageable);
    
    /**
     * Find posts by car make and status
     * @param carMake the car make to filter by
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts matching the criteria
     */
    Page<Post> findByCarMakeAndStatus(String carMake, PostStatus status, Pageable pageable);
    
    /**
     * Find posts by car make, model and status
     * @param carMake the car make to filter by
     * @param carModel the car model to filter by
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts matching the criteria
     */
    Page<Post> findByCarMakeAndCarModelAndStatus(String carMake, String carModel, PostStatus status, Pageable pageable);
    
    /**
     * Find posts by car make, model, year and status
     * @param carMake the car make to filter by
     * @param carModel the car model to filter by
     * @param carYear the car year to filter by
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts matching the criteria
     */
    Page<Post> findByCarMakeAndCarModelAndCarYearAndStatus(String carMake, String carModel, Integer carYear, PostStatus status, Pageable pageable);
    
    /**
     * Search posts by keyword in title or content
     * @param keyword the keyword to search for
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts matching the search criteria
     */
    @Query("SELECT p FROM Post p WHERE p.status = :status AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, 
                                @Param("status") PostStatus status, 
                                Pageable pageable);
    
    /**
     * Find posts within a price range
     * @param minPrice minimum price
     * @param maxPrice maximum price
     * @param status the post status to filter by
     * @param pageable pagination information
     * @return Page of posts within the price range
     */
    @Query("SELECT p FROM Post p WHERE p.status = :status AND " +
           "p.price >= :minPrice AND p.price <= :maxPrice")
    Page<Post> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("status") PostStatus status,
                                 Pageable pageable);
    
    /**
     * Get distinct car makes from all posts
     * @return List of distinct car makes
     */
    @Query("SELECT DISTINCT p.carMake FROM Post p WHERE p.carMake IS NOT NULL ORDER BY p.carMake")
    List<String> findDistinctCarMakes();
    
    /**
     * Get distinct car models for a specific make
     * @param carMake the car make to filter by
     * @return List of distinct car models
     */
    @Query("SELECT DISTINCT p.carModel FROM Post p WHERE p.carMake = :carMake AND p.carModel IS NOT NULL ORDER BY p.carModel")
    List<String> findDistinctCarModelsByMake(@Param("carMake") String carMake);
    
    /**
     * Get distinct car years from all posts
     * @return List of distinct car years
     */
    @Query("SELECT DISTINCT p.carYear FROM Post p WHERE p.carYear IS NOT NULL ORDER BY p.carYear DESC")
    List<Integer> findDistinctCarYears();
}
