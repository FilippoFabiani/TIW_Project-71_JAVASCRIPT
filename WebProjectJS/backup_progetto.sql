-- MySQL dump 10.13  Distrib 8.0.45, for macos15 (arm64)
--
-- Host: localhost    Database: dbProvaTiw
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Mese`
--

DROP TABLE IF EXISTS `Mese`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Mese` (
  `mese` int NOT NULL,
  PRIMARY KEY (`mese`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Mese`
--

LOCK TABLES `Mese` WRITE;
/*!40000 ALTER TABLE `Mese` DISABLE KEYS */;
/*!40000 ALTER TABLE `Mese` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Position`
--

DROP TABLE IF EXISTS `Position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Position` (
  `position` varchar(32) NOT NULL,
  PRIMARY KEY (`position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Position`
--

LOCK TABLES `Position` WRITE;
/*!40000 ALTER TABLE `Position` DISABLE KEYS */;
INSERT INTO `Position` VALUES ('admin'),('technician');
/*!40000 ALTER TABLE `Position` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Progetto`
--

DROP TABLE IF EXISTS `Progetto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Progetto` (
  `titolo` varchar(255) NOT NULL,
  `durata` int NOT NULL,
  `stato` varchar(255) NOT NULL,
  `responsabile` int NOT NULL,
  `amministratore` int NOT NULL,
  PRIMARY KEY (`titolo`),
  KEY `fk_Progetto_Tecnico1_idx` (`responsabile`),
  KEY `fk_Progetto_User1_idx` (`amministratore`),
  CONSTRAINT `fk_Progetto_Tecnico1` FOREIGN KEY (`responsabile`) REFERENCES `Tecnico` (`id`),
  CONSTRAINT `fk_Progetto_User1` FOREIGN KEY (`amministratore`) REFERENCES `User` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Progetto`
--

LOCK TABLES `Progetto` WRITE;
/*!40000 ALTER TABLE `Progetto` DISABLE KEYS */;
INSERT INTO `Progetto` VALUES ('IngSw',4,'CREATO',4,1),('TIW',2,'CREATO',4,2);
/*!40000 ALTER TABLE `Progetto` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_progetto_insert` AFTER INSERT ON `progetto` FOR EACH ROW begin
	call NuovoResponsabile (NEW.responsabile);
end */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `Task`
--

DROP TABLE IF EXISTS `Task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Task` (
  `numero_ordine` int NOT NULL,
  `titolo` varchar(255) NOT NULL,
  `descrizione` varchar(255) NOT NULL,
  `wp` int NOT NULL,
  `progetto` varchar(255) NOT NULL,
  `Mese_inizio` int DEFAULT NULL,
  `Mese_fine` int DEFAULT NULL,
  PRIMARY KEY (`numero_ordine`,`wp`,`progetto`),
  KEY `fk_Task_Work_Package1_idx` (`wp`,`progetto`),
  KEY `fk_Task_Mese1_idx` (`Mese_inizio`),
  KEY `fk_Task_Mese2_idx` (`Mese_fine`),
  CONSTRAINT `fk_Task_Mese1` FOREIGN KEY (`Mese_inizio`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Task_Mese2` FOREIGN KEY (`Mese_fine`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Task_Work_Package1` FOREIGN KEY (`wp`, `progetto`) REFERENCES `Work_Package` (`numero_ordine`, `progetto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Task`
--

LOCK TABLES `Task` WRITE;
/*!40000 ALTER TABLE `Task` DISABLE KEYS */;
INSERT INTO `Task` VALUES (1,'Home_implementation','Creazione dello sfondo della home e implementazione bottoni',1,'IngSw',1,2),(2,'prova trigger','questa è la prova per un trigger',1,'IngSw',NULL,NULL);
/*!40000 ALTER TABLE `Task` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_id_task` BEFORE INSERT ON `task` FOR EACH ROW BEGIN
    DECLARE prox_id INT;
    
    SELECT COALESCE(MAX(numero_ordine), 0) + 1 INTO prox_id
    FROM Task
    WHERE wp = NEW.wp and progetto = NEW.progetto;
    
    SET NEW.numero_ordine = prox_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_fine_mese_task_valido` BEFORE UPDATE ON `task` FOR EACH ROW begin
	
    declare durata_progetto int;
    
    SELECT durata INTO durata_progetto
    FROM Progetto
    WHERE titolo = NEW.progetto;
    
    if NOT (OLD.Mese_fine <=> NEW.Mese_fine) then
		
        if NEW.Mese_fine > durata_progetto then
			SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'Errore: mese di fine non valido!';
        end if;
	end if;
    
end */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `Task_ha_Tecnico`
--

DROP TABLE IF EXISTS `Task_ha_Tecnico`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Task_ha_Tecnico` (
  `task` int NOT NULL,
  `wp` int NOT NULL,
  `progetto` varchar(255) NOT NULL,
  `collaboratore` int NOT NULL,
  `ore_effettive` int DEFAULT NULL,
  `mese` int NOT NULL,
  PRIMARY KEY (`task`,`wp`,`progetto`,`collaboratore`,`mese`),
  KEY `fk_Tecnico_has_Task_Task1_idx` (`task`,`wp`,`progetto`),
  KEY `fk_Task_ha_Tecnico_Tecnico1_idx` (`collaboratore`),
  KEY `fk_Task_ha_Tecnico_Mese1_idx` (`mese`),
  CONSTRAINT `fk_Task_ha_Tecnico_Mese1` FOREIGN KEY (`mese`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Task_ha_Tecnico_Tecnico1` FOREIGN KEY (`collaboratore`) REFERENCES `Tecnico` (`id`),
  CONSTRAINT `fk_Tecnico_has_Task_Task1` FOREIGN KEY (`task`, `wp`, `progetto`) REFERENCES `Task` (`numero_ordine`, `wp`, `progetto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Task_ha_Tecnico`
--

LOCK TABLES `Task_ha_Tecnico` WRITE;
/*!40000 ALTER TABLE `Task_ha_Tecnico` DISABLE KEYS */;
/*!40000 ALTER TABLE `Task_ha_Tecnico` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Task_has_Mese`
--

DROP TABLE IF EXISTS `Task_has_Mese`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Task_has_Mese` (
  `mese` int NOT NULL,
  `ore_previste` int DEFAULT NULL,
  `task` int NOT NULL,
  `wp` int NOT NULL,
  `progetto` varchar(255) NOT NULL,
  PRIMARY KEY (`mese`,`task`,`wp`,`progetto`),
  KEY `fk_Task_has_Mese_Mese1_idx` (`mese`),
  KEY `fk_Task_has_Mese_Task1_idx` (`task`,`wp`,`progetto`),
  CONSTRAINT `fk_Task_has_Mese_Mese1` FOREIGN KEY (`mese`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Task_has_Mese_Task1` FOREIGN KEY (`task`, `wp`, `progetto`) REFERENCES `Task` (`numero_ordine`, `wp`, `progetto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Task_has_Mese`
--

LOCK TABLES `Task_has_Mese` WRITE;
/*!40000 ALTER TABLE `Task_has_Mese` DISABLE KEYS */;
/*!40000 ALTER TABLE `Task_has_Mese` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Tecnico`
--

DROP TABLE IF EXISTS `Tecnico`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Tecnico` (
  `nome` varchar(32) NOT NULL,
  `cognome` varchar(32) NOT NULL,
  `foto` varchar(255) DEFAULT NULL,
  `id` int NOT NULL,
  `isManager` tinyint(1) DEFAULT '0',
  `isCollaborator` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_Tecnico_User1_idx` (`id`),
  CONSTRAINT `fk_Tecnico_User1` FOREIGN KEY (`id`) REFERENCES `User` (`id`),
  CONSTRAINT `tech_not_false` CHECK ((((0 <> `isManager`) is true) or ((0 <> `isCollaborator`) is true)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Tecnico`
--

LOCK TABLES `Tecnico` WRITE;
/*!40000 ALTER TABLE `Tecnico` DISABLE KEYS */;
INSERT INTO `Tecnico` VALUES ('Andrea','Coppola',NULL,3,0,1),('Francesco Paolo','Costamante',NULL,4,1,0);
/*!40000 ALTER TABLE `Tecnico` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `User` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `password` varchar(45) NOT NULL,
  `position` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`),
  KEY `fk_User_Position1_idx` (`position`),
  CONSTRAINT `fk_User_Position1` FOREIGN KEY (`position`) REFERENCES `Position` (`position`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `User`
--

LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` VALUES (1,'nicUser','nicPass','admin'),(2,'filUser','filPass','admin'),(3,'andreUser','andrePass','technician'),(4,'fraUser','fraPass','technician'),(5,'gennaUser','gennaPass',NULL);
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Work_Package`
--

DROP TABLE IF EXISTS `Work_Package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Work_Package` (
  `numero_ordine` int NOT NULL,
  `titolo` varchar(255) NOT NULL,
  `progetto` varchar(255) NOT NULL,
  `Mese_inizio` int DEFAULT NULL,
  `Mese_fine` int DEFAULT NULL,
  PRIMARY KEY (`numero_ordine`,`progetto`),
  KEY `fk_Work_Package_Progetto1_idx` (`progetto`),
  KEY `fk_Work_Package_Mese1_idx` (`Mese_inizio`),
  KEY `fk_Work_Package_Mese2_idx` (`Mese_fine`),
  CONSTRAINT `fk_Work_Package_Mese1` FOREIGN KEY (`Mese_inizio`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Work_Package_Mese2` FOREIGN KEY (`Mese_fine`) REFERENCES `Mese` (`mese`),
  CONSTRAINT `fk_Work_Package_Progetto1` FOREIGN KEY (`progetto`) REFERENCES `Progetto` (`titolo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Work_Package`
--

LOCK TABLES `Work_Package` WRITE;
/*!40000 ALTER TABLE `Work_Package` DISABLE KEYS */;
INSERT INTO `Work_Package` VALUES (1,'TUI_IMPLEMENTATION','IngSw',1,NULL),(2,'GUI_IMPLEMENTATION','IngSw',1,NULL);
/*!40000 ALTER TABLE `Work_Package` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_id_wp` BEFORE INSERT ON `work_package` FOR EACH ROW BEGIN
    DECLARE prox_id INT;
    
    SELECT COALESCE(MAX(numero_ordine), 0) + 1 INTO prox_id
    FROM Work_Package
    WHERE progetto = NEW.progetto;
    
    SET NEW.numero_ordine = prox_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_fine_mese_wp_valido` BEFORE UPDATE ON `work_package` FOR EACH ROW begin
	
    declare durata_progetto int;
    
    SELECT durata INTO durata_progetto
    FROM Progetto
    WHERE titolo = NEW.progetto;
    
    if NOT (OLD.Mese_fine <=> NEW.Mese_fine) then
		
        if NEW.Mese_fine > durata_progetto then
			SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'Errore: mese di fine non valido!';
        end if;
	end if;
    
end */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Dumping routines for database 'dbProvaTiw'
--
/*!50003 DROP PROCEDURE IF EXISTS `NuovoResponsabile` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `NuovoResponsabile`(in id int)
begin
	declare num_progetti int;
    declare is_null boolean;
    
	SELECT (position is null) INTO is_null FROM User
    WHERE User.id = id;
    
    -- non è stato ancora associato il suo profilo di tecnico --
    if (is_null ) then
    
        INSERT INTO Tecnico (id, isManager)
        VALUES (id, TRUE);
        
        UPDATE User
        SET position = 'technician'
        WHERE User.id = id;
        
    else
    
		SELECT count(*) INTO num_progetti FROM Progetto 
		WHERE manager = id;
		
		if num_progetti = 0 then
			UPDATE Tecnico
			SET isManager = false
			WHERE Tecnico.id = id;
		
		else
			UPDATE Tecnico
			SET isManager = true
			WHERE Tecnico.id = id;
			
		end if;
	end if;
    
    end ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-03 16:35:34
