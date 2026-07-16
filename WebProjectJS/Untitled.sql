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
		WHERE responsabile = id;
		
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
    
    end