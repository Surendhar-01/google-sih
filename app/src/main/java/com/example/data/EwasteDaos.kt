package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialLotDao {
    @Query("SELECT * FROM material_lots ORDER BY collectionTimestamp DESC")
    fun getAllLots(): Flow<List<MaterialLotEntity>>

    @Query("SELECT * FROM material_lots WHERE lotId = :id")
    suspend fun getLotById(id: String): MaterialLotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: MaterialLotEntity)

    @Update
    suspend fun updateLot(lot: MaterialLotEntity)

    @Query("DELETE FROM material_lots WHERE lotId = :id")
    suspend fun deleteLot(id: String)

    @Query("SELECT * FROM material_lots WHERE isSynced = 0")
    fun getUnsyncedLots(): Flow<List<MaterialLotEntity>>

    @Query("SELECT COUNT(*) FROM material_lots WHERE isSynced = 0")
    suspend fun getUnsyncedLotsCount(): Int

    @Query("SELECT * FROM material_lots WHERE isSynced = 0")
    suspend fun getUnsyncedLotsNow(): List<MaterialLotEntity>

    @Query("UPDATE material_lots SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllLotsSynced()

    @Query("SELECT * FROM material_lots WHERE categoryName = :category AND weightKg = :weightKg AND collectionTimestamp > :since LIMIT 1")
    suspend fun findRecentDuplicate(category: String, weightKg: Double, since: Long): MaterialLotEntity?
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM price_records ORDER BY prevailingBuyRate DESC")
    fun getAllPrices(): Flow<List<PriceRecordEntity>>

    @Query("SELECT * FROM price_records WHERE location = :location")
    fun getPricesByLocation(location: String): Flow<List<PriceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceRecordEntity>)
}

@Dao
interface RecyclerDao {
    @Query("SELECT * FROM authorized_recyclers ORDER BY distanceKm ASC")
    fun getAllRecyclers(): Flow<List<RecyclerEntity>>

    @Query("SELECT * FROM authorized_recyclers WHERE city = :city")
    fun getRecyclersByCity(city: String): Flow<List<RecyclerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecyclers(recyclers: List<RecyclerEntity>)
}

@Dao
interface TransactionLedgerDao {
    @Query("SELECT * FROM transaction_ledger ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionLedgerEntity>>

    @Query("SELECT * FROM transaction_ledger ORDER BY timestamp DESC")
    suspend fun getAllTransactionsNow(): List<TransactionLedgerEntity>

    @Query("SELECT * FROM transaction_ledger WHERE lotId = :lotId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getTransactionByLot(lotId: String): TransactionLedgerEntity?

    @Query("SELECT SUM(totalAmountInr) FROM transaction_ledger WHERE isSettled = 1")
    fun getTotalSettledEarnings(): Flow<Double?>

    @Query("SELECT SUM(totalAmountInr) FROM transaction_ledger WHERE isSettled = 0")
    fun getPendingDues(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionLedgerEntity)

    @Query("SELECT COALESCE(SUM(totalAmountInr), 0) FROM transaction_ledger WHERE isSettled = 1 AND timestamp >= :startMillis AND timestamp < :endMillis")
    fun getSettledEarningsBetween(startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transaction_ledger WHERE isSettled = 1")
    fun getCompletedTxnCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_ledger WHERE isSettled = 0")
    fun getPendingTxnCount(): Flow<Int>
}

@Dao
interface LotPhotoDao {
    @Query("SELECT * FROM lot_photos WHERE lotId = :lotId ORDER BY createdAt ASC")
    fun getPhotosForLot(lotId: String): Flow<List<LotPhotoEntity>>

    @Query("SELECT * FROM lot_photos WHERE uploadStatus = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingPhotos(): Flow<List<LotPhotoEntity>>

    @Query("SELECT * FROM lot_photos WHERE uploadStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingPhotosNow(): List<LotPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: LotPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<LotPhotoEntity>)

    @Query("UPDATE lot_photos SET uploadStatus = :status, remotePath = :remotePath WHERE photoId = :photoId")
    suspend fun updateStatus(photoId: String, status: String, remotePath: String?)

    @Query("UPDATE lot_photos SET lotId = :lotId WHERE lotId = :oldLotId")
    suspend fun attachPhotosToLot(oldLotId: String, lotId: String)

    @Query("DELETE FROM lot_photos WHERE photoId = :photoId")
    suspend fun deletePhoto(photoId: String)

    @Query("SELECT COUNT(*) FROM lot_photos WHERE photoId = :photoId")
    suspend fun photoExists(photoId: String): Int
}

@Dao
interface CollectorLocationDao {
    @Query("SELECT * FROM collector_locations WHERE collectorUserId = :userId LIMIT 1")
    fun getLocation(userId: String): Flow<CollectorLocationEntity?>

    @Query("SELECT * FROM collector_locations WHERE collectorUserId = :userId LIMIT 1")
    suspend fun getLocationNow(userId: String): CollectorLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: CollectorLocationEntity)
}

@Dao
interface ConnectionRequestDao {
    @Query("SELECT * FROM connection_requests ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ConnectionRequestEntity>>

    @Query("SELECT * FROM connection_requests WHERE status = 'PENDING' OR status = 'ACCEPTED' ORDER BY createdAt DESC")
    fun getActive(): Flow<List<ConnectionRequestEntity>>

    @Query("SELECT * FROM connection_requests ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<ConnectionRequestEntity>

    @Query("SELECT * FROM connection_requests WHERE recyclerId = :recyclerId AND collectorUserId = :collectorUserId AND status = 'PENDING' LIMIT 1")
    suspend fun getExistingPending(collectorUserId: String, recyclerId: String): ConnectionRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(req: ConnectionRequestEntity)

    @Query("UPDATE connection_requests SET status = :status, updatedAt = :updatedAt WHERE requestId = :requestId")
    suspend fun updateStatus(requestId: String, status: String, updatedAt: Long)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPending(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<QuotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quotation: QuotationEntity)

    @Query("UPDATE quotations SET status = :status, respondedAt = :respondedAt WHERE quotationId = :quotationId")
    suspend fun updateStatus(quotationId: String, status: String, respondedAt: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    suspend fun getAllNow(): List<AuditLogEntity>

    @Insert
    suspend fun insert(log: AuditLogEntity)
}

@Dao
interface SafetyGuidelineDao {
    @Query("SELECT * FROM safety_guidelines ORDER BY id ASC")
    fun getAllGuidelines(): Flow<List<SafetyGuidelineEntity>>

    @Query("SELECT * FROM safety_guidelines WHERE id = :id")
    suspend fun getGuidelineById(id: String): SafetyGuidelineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuidelines(guidelines: List<SafetyGuidelineEntity>)
}

