package dk.statsbiblioteket.netarchivesuite.archon.facade;

import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.netarchivesuite.archon.persistence.ArcVO;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.ArchonStorage;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.StringListWrapper;

public class ArchonFacade {


	public static String nextShardID() throws Exception{		

		ArchonStorage storage = new ArchonStorage();
		try {
			int nextShardId = storage.nextShardId();
			storage.commit();
			return ""+nextShardId;
		
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static ArcVO getArcById( String arcID) throws Exception{
		ArchonStorage storage = new ArchonStorage();
		try {
			return storage.getArcByID(arcID);	
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void addARC(String arcID) throws Exception{
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.addARC(arcID);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void addOrUpdateARC(String arcID) throws Exception{
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.addOrUpdateARC(arcID);    
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static String nextARC( String shardID) throws Exception  {
		ArchonStorage storage = new ArchonStorage();
		try {
			String nextARC = storage.nextARC(shardID);
			storage.commit();
			return nextARC;
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void setARCState(String arcID ,  String state) throws Exception     {
		ArchonStorage storage = new ArchonStorage();
		try {
			ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);
			storage.setARCState(arcID, stateEnum);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void clearIndexing(String shardID) throws Exception {
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.clearIndexing(shardID);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void resetShardID(String shardID) throws Exception {
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.resetShardId(shardID);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void removeARC( String arcID) throws Exception {
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.removeARC(arcID);
			storage.commit();    	
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static StringListWrapper getShardIDs() throws Exception  {
		ArchonStorage storage = new ArchonStorage();
		try {
			List<String> shardIds = storage.getShardIDs();
			StringListWrapper wrapper = new StringListWrapper(); //JSON,XML can not marshal List, need a wrapping object
			wrapper.setValues((ArrayList<String>) shardIds); 
			return wrapper;
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static StringListWrapper getARCFiles(String shardID) throws Exception  {
		ArchonStorage storage = new ArchonStorage();   
		try {
			List<String> ARCFiles = storage.getARCFiles(shardID);
			StringListWrapper wrapper = new StringListWrapper(); //JSON,XML can not marshal List, need a wrapping object
			wrapper.setValues((ArrayList<String>) ARCFiles); 
			return wrapper;
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void setARCProperties(String arcID, String shardID,String state,int priority) throws Exception  {
		ArchonStorage storage = new ArchonStorage();
		try {
			ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);        
			storage.setARCProperties(arcID, shardID, stateEnum, priority);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void setARCPriority(String arcID, int priority) throws Exception  {    	
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.setARCPriority(arcID, priority);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}

	public static void resetArcWithPriority(String arcID,int priority) throws Exception  {
		ArchonStorage storage = new ArchonStorage();
		try {
			storage.resetArcWithPriorityStatement(arcID, priority);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}


	public static void setShardState( String shardID, String state, int priority) throws Exception  {
		ArchonStorage storage = new ArchonStorage();   
		try {
			ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);
			storage.setShardState(shardID, stateEnum, priority);
			storage.commit();
		} catch (Exception e) {
			storage.rollback();
			throw e;
		} finally {
			storage.close();
		}
	}


}

