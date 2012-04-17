/*******************************************************************************
 * Copyright 2012 University of Southern California
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/

package edu.isi.karma.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;

public class ServiceRepository {
	
	public final String SERVICE_REPOSITORY_DIR = "service_repository/services/";
	public final String TRIPLE_DATASET_DIR = "service_repository/dataset/";
	
	static Logger logger = Logger.getLogger(ServiceRepository.class);

	private Dataset dataset;

	private static ServiceRepository _InternalInstance = null;
	public static ServiceRepository Instance()
	{
		if (_InternalInstance == null)
		{
			_InternalInstance = new ServiceRepository();

			File serviceRepository = new File(_InternalInstance.SERVICE_REPOSITORY_DIR);
			if (!serviceRepository.exists())
				serviceRepository.mkdir();
			
			_InternalInstance.createRepository();
		}
		return _InternalInstance;
	}
	
	public void createRepository() {
		Location location = new Location(this.TRIPLE_DATASET_DIR);
		this.dataset = TDBFactory.createDataset(location);
//		TDB.getContext().set(TDB.symUnionDefaultGraph, true);
	}
	
	public void addModel(Model m, String name) {
		if (name == null) {
			logger.info("cannot add the model because the given name is null.");
			return;
		}
		
		Model namedModel = this.dataset.getNamedModel(name); 
		namedModel.removeAll();
		namedModel.add(m.listStatements());
		namedModel.setNsPrefixes(m.getNsPrefixMap());
		namedModel.commit();
		TDB.sync(this.dataset);
	}
	
	/**
	 * returns the union of all named models.
	 * @return
	 */
	public Model getModel() {

//		1) tdbDataset.getNamedModel("urn:x-arq:UnionGraph") does always return the union graph (even when
//		TDB.getContext().set(TDB.symUnionDefaultGraph, true) is NOT set).
//		2) tdbDataset.getDefaultModel() does always return the 'real' default graph which is different/independent of (1).
//		3) so only in context of a SPARQL query the'default graph' contains the union of all named graphs.
				
		return this.dataset.getNamedModel("urn:x-arq:UnionGraph") ;
	}
	
	public Model getNamedModel(String name) {
		if (name == null) {
			logger.info("cannot get the model because the given name is null.");
			return null;
		}
		
		if (!this.dataset.containsNamedModel(name)) {
			logger.info("The model: " + name + " does not exist in the service repository.");
			return null;
		}
		return this.dataset.getNamedModel(name);
	}
	
	public void clearNamedModel(String name) {
		if (name == null) {
			logger.info("cannot clear the model because the given name is null.");
			return;
		}
		
		if (!this.dataset.containsNamedModel(name)) {
			logger.info("The model " + name + " does not exist in the service repository.");
			return;
		}
		this.dataset.getNamedModel(name).removeAll();
		this.dataset.getNamedModel(name).commit();
	}
	
	/**
	 * imports the named model from a directory or a file
	 * @param lang The language of the file specified by the lang argument. 
	 * Predefined values are "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") and "N3". 
	 * The default value, represented by null is "RDF/XML".
	 * @param path
	 */
	public void importModel(File file, String lang) {
		if (!file.exists()) {
			logger.error("cannot find the file/dir at " + file.getAbsolutePath());
			return;
		}
		
		if (file.isFile()) {
			importModelFromSingleFile(file, lang);
		} else if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				importModelFromSingleFile(f, lang);
			}
		}
		

	}
	
	private void importModelFromSingleFile(File file, String lang) {
		Model m = ModelFactory.createDefaultModel();
		
		try {
			InputStream s = new FileInputStream(file);
			m.read(s, null);
			
			this.addModel(m, file.getName());
			
			logger.info("The model " + file.getPath() + " successfully imported to repository");
			
		} catch (Throwable t) {
			logger.error("Error reading the model file!", t);
			return;
		}		
	}
	
//	public void clearResourceTriples(String modelName, String uri) {
//		if (uri == null) {
//			logger.info("cannot clear the resource because the given uri is null.");
//			return;
//		}
//		
//		Resource r = this.getModel().getResource(uri);
//		if (r == null) {
//			logger.info("The model does not contain the resource " + uri);
//			return;
//		}
//		
//		Model m = r.getModel();
//		if (m == null) {
//			logger.info("No model is associated to resource " + uri);
//			return;
//		}
//		
//		//TODO
//		
//		m.commit();
//	}

}