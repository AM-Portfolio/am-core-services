import 'package:test/test.dart';
import 'package:am_analysis_client/api.dart';

// Simple Integration Test
void main() {
  group('Analysis Flutter SDK Verification', () {
    late AnalysisControllerApi api;

    setUp(() {
      final client = ApiClient(basePath: 'http://localhost:8093');
      api = AnalysisControllerApi(client);
    });

    test('getTopMoversByCategory connectivity check', () async {
      print('Verifying Flutter SDK connectivity to http://localhost:8093...');
      
      try {
        final response = await api.getTopMoversByCategory(
           'Bearer test-token', // Authorization
           'EQUITY',            // type 
           timeFrame: '1D',
        );
        
        expect(response, isNotNull);
        print('SUCCESS: Received 200 OK from API');
        
      } on ApiException catch (e) {
        print('API Exception: Code=${e.code}, Message=${e.message}');
        
        // Connectivity Verification Logic
        if (e.code == 401 || e.code == 403) {
          print('SUCCESS: Reached API (Auth Rejected as expected)');
        } else if (e.code == 0) {
          // Connection Refused / ClientException
          fail('FAILED: Could not connect to API. Is "am-analysis" running on port 8093?');
        } else {
           print('SUCCESS: Reached API with code ${e.code}');
        }
      } catch (e) {
        fail('FAILED: Unexpected exception: $e');
      }
    });
  });
}
