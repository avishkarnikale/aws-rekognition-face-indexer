package faceid;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DeleteFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteFacesResponse;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.FaceRecord;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.IndexFacesResponse;
import software.amazon.awssdk.services.rekognition.model.QualityFilter;
import software.amazon.awssdk.services.rekognition.model.Reason;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;
import software.amazon.awssdk.services.rekognition.model.UnindexedFace;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class IndexFaces {

	public static void main(String[] args) throws Exception {
		
		StaticCredentialsProvider sc = StaticCredentialsProvider.create(new AwsCredentials() {
			
			@Override
			public String secretAccessKey() {
				// TODO Auto-generated method stub
				return args[0];//"";
			}
			
			@Override
			public String accessKeyId() {
				// TODO Auto-generated method stub
				return args[1];//"";
			}
		});
		
		PublishResponse pr = null;
		try {
			SnsClient amazonSNS = SnsClient.builder()
					  .credentialsProvider(sc).region(Region.EU_WEST_1)
					  .build();
			  pr =  amazonSNS.publish(PublishRequest.builder()
                     .message("Verification Code：56568")
                     .phoneNumber("+919819244208")
                     .topicArn("arn:aws:sns:eu-west-1:423966027140:AWS")
                     
                     .build());
			  
		    		  /**standard().withCredentials(provider).withRegion("us-east-1").build();
		      amazonSNS.publish(
		                new PublishRequest()
		                        .withMessage("Verification Code：565689 [Xwings]")
		                        .withPhoneNumber("+86********")
		                        .withMessageAttributes(smsAttributes)**/
		 } catch (Exception e) {
			 System.out.println(e);
		 }
		System.out.println(pr);
		System.out.println(pr.responseMetadata());
		System.out.println(pr.sdkHttpResponse());
		//face(collectionId, bucket, photo, name, sc);
		
		
		

		
	}

	protected static void face(String collectionId, String bucket, String photo, String name,
			StaticCredentialsProvider sc) throws JsonProcessingException {
		RekognitionClient rekognitionClient = RekognitionClient.builder()
				  .credentialsProvider(sc)
				  .region(Region.EU_WEST_1)
				  .build();
		
		for(String s : rekognitionClient.listCollections().collectionIds() ) {
			System.out.println(s);
		}
		
		
		
		
		System.out.println(rekognitionClient.listCollections().responseMetadata().toString());
		
		
	      
		
		Image image = getImageFromS3Bucket(bucket, photo);
		
		indexFacesFromImage(collectionId, photo, rekognitionClient, image, name);
		
		
		searchFaceByS3Image(collectionId, photo, rekognitionClient, image);
	}

	private static void searchFaceByS3Image(String collectionId, String photo, RekognitionClient rekognitionClient,
			Image image) throws JsonProcessingException {
		SearchFacesByImageRequest searchFacesByImageRequest = SearchFacesByImageRequest.builder()
	              .collectionId(collectionId)
	              .image(image)
	              .faceMatchThreshold(70F)
	              .maxFaces(1)
	              .build();
	           
	       SearchFacesByImageResponse searchFacesByImageResult = 
	               rekognitionClient.searchFacesByImage(searchFacesByImageRequest);

	       printFacesFoundInSearch(photo, searchFacesByImageResult);
	}

	protected static void printFacesFoundInSearch(String photo, SearchFacesByImageResponse searchFacesByImageResult)
			throws JsonProcessingException {
		System.out.println("Faces matching largest face in image from " + photo);
	      List < FaceMatch > faceImageMatches = searchFacesByImageResult.faceMatches();
	      ObjectMapper objectMapper = new ObjectMapper();
	      objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	      for (FaceMatch face: faceImageMatches) {
	    	  
	    	  System.out.println();
	    	  
	    	  System.out.println("We found " + face.face().externalImageId());
	    	  
	    	  
	          System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
	                  .writeValueAsString(face.face()));
	         System.out.println();
	      }
	}

	protected static void indexFacesFromImage(String collectionId, String photo, RekognitionClient rekognitionClient,
			Image image, String name) {
		IndexFacesRequest indexFacesRequest = IndexFacesRequest.builder().image(image).qualityFilter(QualityFilter.AUTO)
				.maxFaces(1).collectionId(collectionId).externalImageId(name)
				.detectionAttributesWithStrings("DEFAULT").build();

		IndexFacesResponse indexFacesResult = rekognitionClient.indexFaces(indexFacesRequest);

		System.out.println("Results for " + photo);
		System.out.println("Faces indexed:");
		identified(indexFacesResult);

		List<UnindexedFace> unindexedFaces = indexFacesResult.unindexedFaces();
		notIndexed(unindexedFaces);
	}

	private static Image getImageFromS3Bucket(String bucket, String photo) {
		S3Object s3 = S3Object.builder().name(photo).bucket(bucket).build();
		Image image = Image.builder().s3Object(s3).build();
		return image;
	}

	protected static void deleteFaceWithFaceID(RekognitionClient rekognitionClient, String collectionId, Collection<String> faces) {
		DeleteFacesRequest deleteFacesRequest = DeleteFacesRequest.builder()
	              .collectionId(collectionId)
	              .faceIds(faces)
	              .build();
	     
	      DeleteFacesResponse deleteFacesResult=rekognitionClient.deleteFaces(deleteFacesRequest);
	      
	     
	      List < String > faceRecords = deleteFacesResult.deletedFaces();
	      System.out.println(Integer.toString(faceRecords.size()) + " face(s) deleted:");
	      for (String face: faceRecords) {
	         System.out.println("FaceID: " + face);
	      }
	}

	private static void notIndexed(List<UnindexedFace> unindexedFaces) {
		System.out.println("Faces not indexed:");
		for (UnindexedFace unindexedFace : unindexedFaces) {
			System.out.println("  Location:" + unindexedFace.faceDetail().boundingBox().toString());
			System.out.println("  Reasons:");
			for (Reason reason : unindexedFace.reasons()) {
				System.out.println("   " + reason);
			}
		}
	}

	private static void identified(IndexFacesResponse indexFacesResult) {
		List<FaceRecord> faceRecords = indexFacesResult.faceRecords();
		for (FaceRecord faceRecord : faceRecords) {
			System.out.println("  Face ID: " + faceRecord.face().faceId());
			System.out.println("  Location:" + faceRecord.faceDetail().boundingBox());
		}
	}
}